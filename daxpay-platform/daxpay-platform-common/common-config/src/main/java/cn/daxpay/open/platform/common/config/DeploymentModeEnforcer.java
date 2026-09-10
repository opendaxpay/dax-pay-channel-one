package cn.daxpay.open.platform.common.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

/// # 通道适配子应用生产部署模式启动期校验器
///
/// 裁剪拷贝自主应用 `dax-pay-open` 的 `common-config` 模块同名类(仅保留通道子应用真实存在的校验项)。
/// 主应用校验 9 项(沙箱/超管/异常详情/Swagger/CORS/actuator/health/放行路径等), 通道子应用没有
/// 认证体系、springdoc、全局沙箱开关、CORS 等设施, 故只保留 **actuator 端点暴露** 与 **health 详情**
/// 两项校验。后续主应用 Enforcer 升级时, 本类需手动同步(仅同步这两项校验逻辑即可)。
///
/// 通过 SPI(`META-INF/spring.factories`, 键 `org.springframework.boot.EnvironmentPostProcessor`) 注册,
/// 在 [ConfigurableEnvironment] 就绪、ApplicationContext 创建之前运行, 是 Spring Boot 官方的
/// fail-fast 扩展点。
///
/// 工作流程:
/// - 推断部署模式: 显式 `daxpay.platform.deployment.mode` > 按 `spring.profiles.active` 推断(含 prod → PROD, 否则 → DEV)
/// - `PROD` 模式下校验 actuator 端点暴露与 health 详情, 收集所有违规后一次性抛 [IllegalStateException] 拒绝启动
/// - `DEV` 模式下完全跳过校验, 保持现有开发体验
///
/// 部署模式与主应用联动: installer 通过共享 `.env.biz` 把同一个 `DEPLOY_MODE` 注入主应用与通道子应用容器,
/// 切换主应用部署模式后, 重启通道子应用容器即自动跟随同值。
///
/// 设计原则: **只校验、不覆盖**。
public class DeploymentModeEnforcer implements EnvironmentPostProcessor {

    /// actuator 敏感端点黑名单(暴露这些会泄露环境变量/线程栈/内部结构/可关停应用)
    ///
    /// 不含 `metrics`/`info`/`health`/`prometheus`(由项目 `application-prod.yml` 显式放行)
    private static final Set<String> SENSITIVE_ACTUATOR_ENDPOINTS = Set.of(
            "env", "heapdump", "threaddump", "beans", "loggers",
            "mappings", "configprops", "caches", "conditions",
            "scheduledtasks", "startup", "httpexchanges",
            "auditevents", "shutdown", "prometheus"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        // 推断部署模式
        String mode = resolveMode(env);
        if (!"PROD".equals(mode)) {
            // DEV 模式不校验, 保持现状
            return;
        }

        // ERROR 级校验: 收集所有违规后一次性 fail fast(避免"修一个重启再发现一个"的折磨)
        List<String> errors = new ArrayList<>();
        checkActuatorEndpoints(env, errors);
        checkHealthShowDetails(env, errors);

        // 有 ERROR 则拒绝启动(失败信息由 Spring Boot 启动失败机制打印, 通过则静默)
        if (!errors.isEmpty()) {
            throw new IllegalStateException(buildFailureMessage(errors));
        }
    }

    /// 推断部署模式: 显式配置优先, 否则按 active profile 推断
    private String resolveMode(ConfigurableEnvironment env) {
        String explicit = env.getProperty("daxpay.platform.deployment.mode");
        if (Objects.nonNull(explicit) && !explicit.isBlank()) {
            return explicit.toUpperCase();
        }
        // 未显式配置: 含 prod profile → PROD, 否则 → DEV
        for (String profile : env.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return "PROD";
            }
        }
        return "DEV";
    }

    /// 校验 actuator 端点暴露不含敏感端点
    private void checkActuatorEndpoints(ConfigurableEnvironment env, List<String> errors) {
        String include = env.getProperty("management.endpoints.web.exposure.include", "");
        Set<String> exposed = Arrays.stream(include.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (exposed.isEmpty()) {
            // 未配置时 Spring Boot 默认仅暴露 health/info, 安全
            return;
        }
        if (exposed.contains("*")) {
            errors.add("  - management.endpoints.web.exposure.include = *  [禁止全量暴露 actuator 端点, 仅允许 health,info,metrics]");
            return;
        }
        List<String> hit = exposed.stream()
                .filter(SENSITIVE_ACTUATOR_ENDPOINTS::contains)
                .sorted()
                .toList();
        if (!hit.isEmpty()) {
            errors.add("  - management.endpoints.web.exposure.include 含敏感端点: " + hit
                    + "  [生产仅允许 health,info,metrics]");
        }
    }

    /// 校验 health 端点不对外暴露详情(always 会泄露内部状态)
    private void checkHealthShowDetails(ConfigurableEnvironment env, List<String> errors) {
        String showDetails = env.getProperty("management.endpoint.health.show-details", "never");
        if ("always".equalsIgnoreCase(showDetails)) {
            errors.add("  - management.endpoint.health.show-details = always  [会暴露内部状态, 改为 never 或 when-authorized]");
        }
    }

    private String buildFailureMessage(List<String> errors) {
        return System.lineSeparator()
                + "----------------------------------------------------------" + System.lineSeparator()
                + "  通道适配子应用生产部署模式(PROD)启动校验失败" + System.lineSeparator()
                + "  检测到 " + errors.size() + " 项配置不合规:" + System.lineSeparator()
                + String.join(System.lineSeparator(), errors) + System.lineSeparator()
                + "----------------------------------------------------------" + System.lineSeparator()
                + "  请在 application-prod.yml / 环境变量中修正上述配置后重启。" + System.lineSeparator()
                + "  如确需在当前环境开启(例如联调), 显式设置 daxpay.platform.deployment.mode=DEV" + System.lineSeparator()
                + "----------------------------------------------------------";
    }
}
