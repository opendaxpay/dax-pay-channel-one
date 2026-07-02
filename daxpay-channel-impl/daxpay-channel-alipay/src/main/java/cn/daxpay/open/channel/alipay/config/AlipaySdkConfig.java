package cn.daxpay.open.channel.alipay.config;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.AlipayRequest;
import com.alipay.api.AlipayResponse;
import com.alipay.api.DefaultAlipayClient;
import cn.daxpay.open.platform.core.exception.SdkCallException;

/// # 支付宝 SDK 客户端构建与调用工具
///
/// 根据主应用下发的通道凭证 [AlipaySdkCredential] 构建 [AlipayClient], 支持公钥模式与证书模式两种鉴权方式。
/// 网关地址未配置时按是否沙箱自动选择。
///
/// ## 接口调用
/// 证书模式([AlipayAuthTypeEnum.CERT]) 的服务器请求必须改用 `certificateExecute`, 否则响应验签失败。
/// `pageExecute`(电脑网站/手机网站支付) 与 `sdkExecute`(APP 支付) 属本地签名, 证书模式由 SDK 内部自动适配,
/// 业务层直接使用 [AlipayClient] 对应方法即可, 无需分发。
public class AlipaySdkConfig {

    /// 根据通道凭证构建 [AlipayClient]
    ///
    /// 构建失败时抛出 [SdkCallException]。证书模式需同时提供应用证书、支付宝公钥证书、根证书。
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
            // 证书模式使用三本证书, 公钥模式使用支付宝公钥
            if (AlipayAuthTypeEnum.fromCode(credential.getAuthType()).isCert()) {
                config.setAppCertContent(credential.getAppCert());
                config.setAlipayPublicCertContent(credential.getAlipayCert());
                config.setRootCertContent(credential.getAlipayRootCert());
            } else {
                config.setAlipayPublicKey(credential.getAlipayPublicKey());
            }
            return new DefaultAlipayClient(config);
        } catch (AlipayApiException e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }

    /// 服务器请求调用(alipay.trade.* 等需实际访问网关的接口)
    ///
    /// 证书模式走 `certificateExecute`, 公钥模式走 `execute`。
    public static <T extends AlipayResponse> T execute(AlipayClient client, AlipaySdkCredential credential,
                                                       AlipayRequest<T> request) throws AlipayApiException {
        return AlipayAuthTypeEnum.fromCode(credential.getAuthType()).isCert()
                ? client.certificateExecute(request)
                : client.execute(request);
    }
}
