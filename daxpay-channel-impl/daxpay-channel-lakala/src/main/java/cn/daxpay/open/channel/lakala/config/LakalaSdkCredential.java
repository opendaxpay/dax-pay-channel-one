package cn.daxpay.open.channel.lakala.config;

import lombok.Data;

/// # 拉卡拉 SDK 凭证
///
/// 主应用从服务商密钥配置(LakalaIsvKeyConfig) + 通道商户绑定(LakalaIsvChannelMerchant) 提取
/// 应用编号 / 证书序列号 / 私钥 / 公钥 / 商户号 / 终端号后组装, 下发给子应用发起拉卡拉 API 调用。
///
/// 仅承载通道调用所需的身份与密钥信息, 不含任何业务字段。
@Data
public class LakalaSdkCredential {

    /// 拉卡拉应用编号(lkl_app_id)
    private String lklAppId;

    /// 商户证书序列号
    private String mchSerialNo;

    /// 商户RSA私钥(PEM 格式 PKCS#8 字符串)
    private String privateKey;

    /// 拉卡拉RSA公钥(PEM 格式, 用于响应验签)
    private String publicKey;

    /// 拉卡拉商户编号(merchantNo)
    private String lakalaMchNo;

    /// 终端号(termNo)
    private String termNo;

    /// 是否沙箱环境
    private Boolean sandbox;
}
