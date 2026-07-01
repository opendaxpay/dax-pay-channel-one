package cn.daxpay.open.channel.alipay.config;

import lombok.Data;

/// # 支付宝 SDK 凭证
///
/// 主应用从进件实体提取 appId / 私钥 / 证书后组装, 下发给子应用用于构建 [com.alipay.api.AlipayClient]。
/// 仅承载通道调用所需的身份与密钥信息, 不含任何业务字段, 与系统内业务配置实体严格区分。
@Data
public class AlipaySdkCredential {
    /// 支付宝应用ID
    private String aliAppId;
    /// 应用私钥
    private String privateKey;
    /// 支付宝公钥(公钥模式使用)
    private String alipayPublicKey;
    /// 应用公钥证书内容(证书模式使用)
    private String appCert;
    /// 支付宝公钥证书内容(证书模式使用)
    private String alipayCert;
    /// 支付宝根证书内容(证书模式使用)
    private String alipayRootCert;
    /// 网关地址(为空时按沙箱标志自动选择)
    private String serverUrl;
    /// 签名类型(默认 RSA2)
    private String signType;
    /// 鉴权方式(publickey 公钥模式 / cert 证书模式)
    private String authType;
    /// 是否沙箱环境
    private Boolean sandbox;
    /// 应用授权令牌(服务商代商户调用时使用)
    private String appAuthToken;
}
