package cn.daxpay.open.channel.alipay.config;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import cn.daxpay.open.platform.core.exception.SdkCallException;

import java.util.Map;

/// # 支付宝 SDK 客户端构建工具
///
/// 根据主应用下发的通道配置 Map 构建 [AlipayClient], 支持公钥模式与证书模式两种鉴权方式。
/// 网关地址未配置时按是否沙箱自动选择。
public class AlipaySdkConfig {

    /// 根据通道配置 Map 构建 [AlipayClient]
    ///
    /// 配置缺失或构建失败时抛出 [SdkCallException]。证书模式(`authType=cert`)需同时提供应用证书、公钥证书、根证书。
    public static AlipayClient buildClient(Map<String, Object> configMap) {
        try {
            AlipayConfigDto dto = mapToDto(configMap);
            AlipayConfig config = new AlipayConfig();
            String serverUrl = dto.getServerUrl();
            if (StrUtil.isBlank(serverUrl)) {
                serverUrl = Boolean.TRUE.equals(dto.getSandbox())
                        ? "https://openapi-sandbox.dl.alipaydev.com/gateway.do"
                        : "https://openapi.alipay.com/gateway.do";
            }
            config.setServerUrl(serverUrl);
            config.setAppId(dto.getAliAppId());
            config.setPrivateKey(dto.getPrivateKey());
            config.setFormat("json");
            config.setCharset("UTF-8");
            config.setSignType("RSA2");
            if ("cert".equals(dto.getAuthType())) {
                config.setAlipayPublicCertContent(dto.getAlipayPublicKey());
                config.setAppCertContent(dto.getAppCert());
                config.setRootCertContent(dto.getAlipayRootCert());
            } else {
                config.setAlipayPublicKey(dto.getAlipayPublicKey());
            }
            return new DefaultAlipayClient(config);
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }

    /// 将配置 Map 映射为 [AlipayConfigDto]
    private static AlipayConfigDto mapToDto(Map<String, Object> map) {
        AlipayConfigDto dto = new AlipayConfigDto();
        dto.setAliAppId(getString(map, "aliAppId"));
        dto.setPrivateKey(getString(map, "privateKey"));
        dto.setAlipayPublicKey(getString(map, "alipayPublicKey"));
        dto.setAppCert(getString(map, "appCert"));
        dto.setAlipayCert(getString(map, "alipayCert"));
        dto.setAlipayRootCert(getString(map, "alipayRootCert"));
        dto.setServerUrl(getString(map, "serverUrl"));
        dto.setSignType(getString(map, "signType"));
        dto.setAuthType(getString(map, "authType"));
        Object sandbox = map.get("sandbox");
        dto.setSandbox(sandbox instanceof Boolean ? (Boolean) sandbox : false);
        dto.setAppAuthToken(getString(map, "appAuthToken"));
        return dto;
    }

    /// 安全地从 Map 中取字符串(空值返回 null)
    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
