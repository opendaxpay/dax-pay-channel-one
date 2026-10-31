package cn.daxpay.open.channel.mock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// # Mock 网关配置
///
/// one 侧唯一配置项: 10400 mock 网关基地址(契约 §九)。
/// 密钥不走 one 配置——凭证由主应用每次请求下发, one 保持无状态。
@Data
@ConfigurationProperties(prefix = "mock.gateway")
public class MockGatewayProperties {

    /// Mock 网关基地址(模拟微信开放平台服务器, dev 默认 10400 本机)
    private String baseUrl = "http://127.0.0.1:10400";
}
