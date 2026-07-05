package cn.daxpay.open.channel.ums.config;

import lombok.Data;

/// # 银联商务 SDK 凭证
///
/// 主应用从通道商户配置表中提取 appId / appKey / 商户号 / 终端号 / 通讯密钥后组装,
/// 随每次请求下发给子应用, 子应用据此动态构建 [cn.daxpay.open.channel.ums.sdk.UmsClient]。
///
/// 银联商务签名无需证书, 仅依赖 appKey(HmacSHA256) 与 secretKey(回调验签 MD5/SHA256)。
@Data
public class UmsSdkCredential {

    /// 银联商务应用 AppId
    private String umsAppId;

    /// 应用密钥(用于 OPEN-BODY-SIG / OPEN-FORM-PARAM 签名的 HmacSHA256 密钥)
    private String appKey;

    /// 商户号(mid)
    private String merchantNo;

    /// 终端号(tid)
    private String terminalNo;

    /// 通讯密钥(用于异步回调验签, MD5/SHA256 拼接密钥)
    private String secretKey;

    /// 是否沙箱环境
    private boolean sandbox;
}
