package cn.daxpay.open.channel.alipay.config;

import lombok.Data;

/// # 支付宝通道配置
///
/// 与主应用下发的 `config` Map 中的 key 一一对应, 用于构建 [com.alipay.api.AlipayClient]。
@Data
public class AlipayConfigDto {
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
