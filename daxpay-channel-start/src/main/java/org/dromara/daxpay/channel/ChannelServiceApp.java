package org.dromara.daxpay.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@SpringBootApplication(scanBasePackages = "org.dromara.daxpay.channel")
public class ChannelServiceApp {

    public static void main(String[] args) throws UnknownHostException {
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
