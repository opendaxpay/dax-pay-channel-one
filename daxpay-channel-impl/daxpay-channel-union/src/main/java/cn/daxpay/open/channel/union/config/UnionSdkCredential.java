package cn.daxpay.open.channel.union.config;

import lombok.Data;

/// # 云闪付 SDK 凭证
///
/// 主应用从通道商户配置表中提取商户号、签名类型与三证书后组装,
/// 随每次请求下发给子应用, 子应用据此动态构建 [cn.daxpay.open.channel.union.sdk.UnionClient]。
///
/// 银联 ACP 采用 RSA2 证书签名(区别于银联商务的 HmacSHA256 无证书模式):
/// - **私钥证书**(PKCS12): 请求签名, 需配套证书密码
/// - **中级证书 / 根证书**(X.509 DER): 回调验签与证书链校验
///
/// 三证书均为 Base64 编码字符串, 由主应用加密存储、下发时解密为明文 Base64。
@Data
public class UnionSdkCredential {

    /// 银联商户号(merId)
    private String merId;

    /// 签名类型(银联 ACP 固定 RSA2)
    private String signType;

    /// 是否证书签名(银联 ACP 固定 true)
    private boolean certSign;

    /// 应用私钥证书(Base64 编码的 PKCS12 字符串)
    private String keyPrivateCert;

    /// 私钥证书密码
    private String keyPrivateCertPwd;

    /// 中级证书(Base64 编码的 X.509 DER)
    private String acpMiddleCert;

    /// 根证书(Base64 编码的 X.509 DER)
    private String acpRootCert;

    /// 是否沙箱环境
    private boolean sandbox;
}
