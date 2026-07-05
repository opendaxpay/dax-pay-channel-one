package cn.daxpay.open.channel.wechat.config;

import lombok.Data;

/// # 微信 SDK 凭证
///
/// 主应用从进件实体(WechatDirectApp + WechatDirectKeyConfig)提取商户号 / 应用ID / 私钥 / 证书后组装,
/// 下发给子应用用于构建 [com.github.binarywang.wxpay.service.WxPayService]。
/// 仅承载通道调用所需的身份与密钥信息, 不含任何业务字段, 与系统内业务配置实体严格区分。
///
/// 鉴权模式:
/// - 支付公钥新模式(优先): 配置了 publicKeyId + publicKey 时启用, 免平台证书自动轮换(微信官方推荐)
/// - 平台证书模式(兜底): publicKeyId 为空时, 由 SDK 自动下载并轮换平台证书
@Data
public class WechatSdkCredential {
    /// 微信商户号
    private String wxMchId;
    /// 微信应用ID(公众号 / 小程序 / APP 的 appId)
    private String wxAppId;
    /// APIv3 密钥(用于回调解密与平台证书下载)
    private String apiKeyV3;
    /// 商户私钥(PEM 格式 PKCS#8 字符串)
    private String privateKey;
    /// 商户证书内容(PEM 格式, 部分场景需要)
    private String privateCert;
    /// 商户证书序列号
    private String certSerialNo;
    /// 支付公钥(PEM 格式, 支付公钥新模式使用, 为空则走平台证书模式)
    private String publicKey;
    /// 支付公钥ID(支付公钥新模式使用)
    private String publicKeyId;
    /// 是否沙箱环境
    private Boolean sandbox;
}
