package cn.daxpay.open.channel.mock.sdk;

import cn.daxpay.open.platform.core.util.RsaKeyLoader;

import java.security.PrivateKey;
import java.security.PublicKey;

/// # PEM 密钥加载工具
///
/// 纯 JDK 实现: 剥离 PEM 头尾与空白, Base64 解码后经 KeyFactory(RSA) 还原密钥。
/// 私钥 PKCS#8/PKCS#1 均支持, 公钥 X.509/PKCS#1 均支持, 与 dev 测试套件密钥形态一致(契约 §二)。
public final class MockPemUtil {

    private MockPemUtil() {
    }

    /// 加载私钥(PEM 原文或裸 Base64, PKCS#8/PKCS#1 均可, 商户私钥请求签名用)
    public static PrivateKey loadPrivateKey(String pem) {
        try {
            return RsaKeyLoader.loadPrivateKey(pem);
        } catch (Exception e) {
            throw new IllegalStateException("RSA 私钥 PEM 解析失败", e);
        }
    }

    /// 加载公钥(PEM 原文或裸 Base64, X.509/PKCS#1 均可, 平台公钥验签用)
    public static PublicKey loadPublicKey(String pem) {
        try {
            return RsaKeyLoader.loadPublicKey(pem);
        } catch (Exception e) {
            throw new IllegalStateException("RSA 公钥 PEM 解析失败", e);
        }
    }
}
