package cn.daxpay.open.channel.wechat.config;

import lombok.Data;

/// # 微信通道配置
///
/// 与主应用下发的 `config` Map 中的 key 一一对应, 用于构建微信支付客户端。
@Data
public class WechatConfigDto {
    /// 微信商户号
    private String wxMchId;
    /// 微信应用ID
    private String wxAppId;
    /// APIv3 密钥
    private String apiKeyV3;
    /// 商户私钥
    private String privateKey;
    /// 商户证书内容
    private String privateCert;
    /// 证书序列号
    private String certSerialNo;
    /// 子商户号(服务商模式)
    private String subMchId;
    /// 子应用ID(服务商模式)
    private String subAppId;
}
