package cn.daxpay.open.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.UnknownHostException;

/// # 通道子应用 One 启动类
///
/// 独立部署的支付通道服务, 接收主应用 dax-pay-open 通过声明式 HTTP 客户端转发的通道请求,
/// 按 `channel` 字段路由到对应实现(支付宝/微信等)。启动后打印健康检查地址。
@Slf4j
@SpringBootApplication
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
        var healthUrl = String.format("Health: http://%s:%s%s/health", host, port, contextPath);
        var localHealthUrl = String.format("Health: http://%s:%s%s/health", "127.0.0.1", port, contextPath);

        String message = System.lineSeparator() +
                "----------------------------------------------------------" + System.lineSeparator() +
                "    " + appInfo + System.lineSeparator() +
                "    " + healthUrl + System.lineSeparator() +
                "    " + localHealthUrl + System.lineSeparator() +
                "----------------------------------------------------------";
        log.info(message);
    }
}
