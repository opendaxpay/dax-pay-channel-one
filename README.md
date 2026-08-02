# DaxPay Channel One — 通道适配子应用

通道适配子应用，用于对接单一第三方支付渠道，可作为新支付渠道接入的模板工程。
主应用(dax-pay-open)通过 HTTP 调用本子服务完成通道对接，拆分目的: 通道 SDK 依赖隔离 + 独立升级 + 弹性伸缩。

## 子模块

| 模块 | 说明 |
|------|------|
| `daxpay-platform` | 基础层聚合(对标主项目 `daxpay-platform`) — 通用契约 + 技术设施 |
| `daxpay-channel-impl` | 通道实现 — 下单、退款、查询、回调(按渠道拆子模块) |
| `daxpay-channel-start` | 启动入口 |

### daxpay-platform 内部结构

| 子模块 | 说明 | 对标主项目 |
|--------|------|-----------|
| `daxpay-platform-core` | 通用契约 — DTO、枚举、异常、结果封装、通道服务接口 | `daxpay-platform-core` |
| `daxpay-platform-common`(pom) | 通用技术设施聚合 | `daxpay-platform-common` |
| ├─ `common-i18n` | JSON 消息源 + 工具类 | `common-i18n` |
| ├─ `common-json` | Jackson 序列化全局配置 | `common-json` |
| └─ `common-util` | 金额换算等工具(PayUtil) | `common-util` |
| `daxpay-platform-service` └ `service-system` | web 组装层 — 全局异常处理(ChannelExceptionHandler) | `service-system` |

## 与主项目的结构对照

本子应用与主项目 `dax-pay-open` 保持相同的分层范式(基础层聚合 + 业务模块 + 启动), 维护时按相同模式对照同步:

```
daxpay-channel-one (根)              daxpay (根)
├── daxpay-platform/  ←基础层        ├── daxpay-platform/   ←基础层
│   ├── platform-core  (通用契约)    │   ├── platform-core
│   ├── platform-common(技术设施)    │   ├── platform-common
│   └── platform-service(system)     │   ├── platform-capability
├── daxpay-channel-impl (通道实现)   │   └── platform-service
└── daxpay-channel-start             ├── daxpay-payment / daxpay-channel (业务)
                                     └── daxpay-start
```

> 本子应用是主项目结构的轻量子集: platform 保留 core + common(i18n/json/util) + service(system), 不要 capability。
> 通用契约(DTO/接口/异常)放入 `daxpay-platform-core`, 便于后续 channel-2/3/4 复用。
> 两边独立 git；产品版本与主应用对齐为 4.0.0-beta1，无 maven 依赖，仅结构对标。

## 技术栈

- Java 25
- Spring Boot 4.1.0
- Lombok / Hutool
- OpenTelemetry(仅日志链路关联)

## 构建

```bash
mvnd clean compile "-Dmaven.test.skip=true" -T 4
```

> 注意: Spring Boot 4.1 起 `-DskipTests` 不再跳过测试的 AOT 处理，需改用 `-Dmaven.test.skip=true`；
> PowerShell 下含 `=` 的 `-D` 参数必须加引号，否则会被拆分。
> 日常编译验证只用 `compile`，禁止 `install`。

## 运行

```bash
cd daxpay-channel-start && mvnd spring-boot:run -Dspring-boot.run.profiles=dev
```

默认端口 20100，Health: http://127.0.0.1:20100/actuator/health

## 配置环境(Profile)

| Profile | 说明 | 监控端点 |
|---------|------|----------|
| `dev`(默认) | 本地开发，业务包 DEBUG，凭证可用环境变量覆盖 | 全开(排障友好) |
| `prod` | 生产，凭证强制环境变量注入，优雅停机 | 收紧(health/info/metrics) |

切换: `-Dspring-boot.run.profiles=prod` 或 `SPRING_PROFILES_ACTIVE=prod`

## 国际化(i18n)

- 采用自定义 `JsonMessageSource` 扫描 `classpath*:i18n/{locale}/**/*.json`，文件路径映射为 key 前缀(如 `channel/error.json` → `channel.error.*`)
- 资源位于 `daxpay-platform/daxpay-platform-common/common-i18n/src/main/resources/i18n/{zh-CN,en-US}/`
- locale 由请求头 `Accept-Language` 决定(zh-CN / en-US)
- 新增翻译需同时维护 `zh-CN` 与 `en-US`

## 序列化(Jackson)

- 由 `daxpay-platform-common/common-json` 的 `JacksonConfiguration` 提供, 对标主项目 `common-json`
- Long → String(防前端精度丢失)、OffsetDateTime → ISO UTC、全局时区 UTC
- 与主项目保持一致的序列化行为, 主子应用通信无格式偏差

## 接入新通道

1. 在 `daxpay-channel-impl` 下新建 `daxpay-channel-xxx` 子模块, 并在 `daxpay-channel-impl/pom.xml` 的 `<modules>` 中登记
2. 通用契约(DaxResult/异常/错误码)已在 `daxpay-platform-core` 定义(`cn.daxpay.open.platform.core.*`); 通道专属配置/DTO 放新模块自身
3. 在新模块中提供通道服务类与 `@RestController`(端点前缀 `/channel/xxx`), 用 `XxxChannelAutoConfig`(`@Configuration` + `@ComponentScan`) 装配
4. 在 `daxpay-channel-start/pom.xml` 中引入新模块依赖
5. 通道专属 i18n 键追加到 `common-i18n` 的 `channel/error.json`(zh-CN + en-US 同步)
6. 主项目 `dax-pay-open` 侧:按 `DaxpayChannelProperties` 路由策略将该通道分配到子应用 `one`

## License

本项目基于 [GNU LGPL v3.0 或更高版本](./LICENSE) 协议开源，同时受[《用户授权使用协议》](./USER-AGREEMENT.txt)约束。在使用前请阅读上述协议，如果不同意请勿进行使用。
