package cn.daxpay.open.channel.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/// # RestClient 配置
///
/// 为通道客户端提供统一的 HTTP 传输基础设施, 基于 Apache HttpClient 5 连接池。
/// 各通道 *Client 通过构造器注入此 RestClient 实例, 直连第三方支付网关。
///
/// 注意: 本 RestClient 仅用于调用第三方支付通道(银联商务等),
/// 不挂载 OTel observation 与业务上下文透传(第三方不参与链路, accept-language/x-client-code 无意义)。
@Configuration
public class RestClientConfiguration {

    /// HC5 连接池 HttpClient
    ///
    /// 连接超时 5s, 套接字读取 30s, 响应总超时 40s, 连接池获取 3s;
    /// 总连接 100, 单主机 20, 连接 TTL 5 分钟, 自动清理空闲 30s 与过期连接。
    /// 支付场景无状态: 关闭 Cookie / 认证缓存 / 自动重试 / 重定向。
    @Bean
    public CloseableHttpClient httpClient() {
        // 连接池配置
        var connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        // 连接配置(TCP 层)
        var connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setSocketTimeout(Timeout.ofSeconds(30))
                .setTimeToLive(Timeout.ofMinutes(5))
                .build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        // 请求配置(HTTP 层)
        var requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(40))
                .setConnectionRequestTimeout(Timeout.ofSeconds(3))
                .build();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(Timeout.ofSeconds(30))
                .disableCookieManagement()
                .disableAuthCaching()
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .build();
    }

    /// 将 HC5 HttpClient 适配为 Spring 的 ClientHttpRequestFactory
    @Bean
    public HttpComponentsClientHttpRequestFactory httpRequestFactory(CloseableHttpClient httpClient) {
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /// 全局 RestClient(HC5 连接池), 直连第三方支付网关
    ///
    /// 直接使用 RestClient.builder(), 不依赖自动装配的 Builder(避免挂载 OTel observation)。
    @Bean
    public RestClient restClient(HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        return RestClient.builder()
                .requestFactory(httpRequestFactory)
                .build();
    }
}
