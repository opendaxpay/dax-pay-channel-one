package org.dromara.daxpay.channel.alipay.config;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import org.dromara.daxpay.channel.common.config.AlipayConfigDto;
import org.dromara.daxpay.channel.core.exception.SdkCallException;

import java.util.Map;

public class AlipaySdkConfig {

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
            config.setAppId(dto.getAppId());
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

    private static AlipayConfigDto mapToDto(Map<String, Object> map) {
        AlipayConfigDto dto = new AlipayConfigDto();
        dto.setAppId(getString(map, "appId"));
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

    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
