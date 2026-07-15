package cn.daxpay.open.channel.alipay.req;

import cn.daxpay.open.channel.alipay.config.AlipaySdkCredential;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/// # 支付宝应用授权令牌换取请求
///
/// 主应用经声明式 HTTP 客户端下发, 子应用调用 `alipay.open.auth.token.app` 用授权码换取 app_auth_token。
/// 凭证使用服务商应用密钥, 不携带子商户 appAuthToken。
@Data
public class AlipayAppAuthTokenReq {

    /// 应用授权码(支付宝回调回传的 app_auth_code)
    @NotBlank(message = "{validation.field.oauthCode.notBlank}")
    private String authCode;

    /// 通道调用凭证(服务商应用密钥/证书)
    private AlipaySdkCredential credential;
}
