package cn.daxpay.open.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.net.InetAddress;
import java.net.UnknownHostException;

/// # 通道子应用 One 启动类
///
/// 独立部署的支付通道服务, 接收主应用 dax-pay-open 通过声明式 HTTP 客户端转发的通道请求,
/// 按 `channel` 字段路由到对应实现(支付宝/微信等)。启动后打印健康检查地址。
///
/// mock 模块(`cn.daxpay.open.channel.mock`)为 DEV 专属: 从包扫描中整体剔除,
/// 仅由 [cn.daxpay.open.channel.mock.MockChannelAutoConfig](@AutoConfiguration + 条件装配)加载;
/// 否则 PROD 下条件类被跳过、同包组件仍被扫描注册, 会因缺少 MockGatewayProperties 启动失败。
@Slf4j
@SpringBootApplication
@ComponentScan(excludeFilters = {
        // 保留 @SpringBootApplication 默认的 TypeExcludeFilter(测试切片/自定义排除扩展点)
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        // 保留默认的自动配置类排除(自动配置只经 imports 装载, 不参与包扫描)
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
        // mock 包整体不做包扫描, 交给 MockChannelAutoConfig 按 daxpay.platform.deployment.mode 条件装配
        @ComponentScan.Filter(type = FilterType.REGEX,
                pattern = "cn\\.daxpay\\.open\\.channel\\.mock\\..*")
})
public class ChannelServiceApp {

    /// 应用入口, 启动 Spring 容器并打印健康检查地址
    static void main(String[] args) throws UnknownHostException {
        var application = SpringApplication.run(ChannelServiceApp.class, args);
        var env = application.getEnvironment();
        var appName = env.getProperty("spring.application.name");
        var host = InetAddress.getLocalHost().getHostAddress();
        var port = env.getProperty("server.port");
        var contextPath = env.getProperty("server.servlet.context-path", "");

        var appInfo = String.format("应用 '%s' 运行成功!", appName);
        var healthUrl = String.format("API: http://%s:%s%s/health", host, port, contextPath);
        var localHealthUrl = String.format("API: http://%s:%s%s/health", "127.0.0.1", port, contextPath);

        String message = System.lineSeparator() +
                "----------------------------------------------------------" + System.lineSeparator() +
                "    " + appInfo + System.lineSeparator() +
                "    " + healthUrl + System.lineSeparator() +
                "    " + localHealthUrl + System.lineSeparator() +
                "----------------------------------------------------------";
        log.info(message);
    }
}
