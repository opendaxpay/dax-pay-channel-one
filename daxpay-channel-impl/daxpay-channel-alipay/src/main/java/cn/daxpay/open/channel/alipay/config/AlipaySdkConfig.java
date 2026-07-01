package cn.daxpay.open.channel.alipay.config;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import cn.daxpay.open.platform.core.exception.SdkCallException;

/// # 支付宝 SDK 客户端构建工具
///
/// 根据主应用下发的通道凭证 [AlipaySdkCredential] 构建 [AlipayClient], 支持公钥模式与证书模式两种鉴权方式。
/// 网关地址未配置时按是否沙箱自动选择。
public class AlipaySdkConfig {

    /// 根据通道凭证构建 [AlipayClient]
    ///
    /// 构建失败时抛出 [SdkCallException]。证书模式(`authType=cert`)需同时提供应用证书、公钥证书、根证书。
    public static AlipayClient buildClient(AlipaySdkCredential credential) {
        try {
            AlipayConfig config = new AlipayConfig();
            String serverUrl = credential.getServerUrl();
            if (StrUtil.isBlank(serverUrl)) {
                serverUrl = Boolean.TRUE.equals(credential.getSandbox())
                        ? "https://openapi-sandbox.dl.alipaydev.com/gateway.do"
                        : "https://openapi.alipay.com/gateway.do";
            }
            config.setServerUrl(serverUrl);
            config.setAppId(credential.getAliAppId());
            config.setPrivateKey(credential.getPrivateKey());
            if ("cert".equals(credential.getAuthType())) {
                config.setAlipayPublicCertContent(credential.getAlipayPublicKey());
                config.setAppCertContent(credential.getAppCert());
                config.setRootCertContent(credential.getAlipayRootCert());
            } else {
                config.setAlipayPublicKey(credential.getAlipayPublicKey());
            }
            return new DefaultAlipayClient(config);
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
