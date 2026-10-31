package cn.daxpay.open.channel.mock.sdk;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/// # PEM 密钥加载工具
///
/// 纯 JDK 实现: 剥离 PEM 头尾与空白, Base64 解码后经 KeyFactory(RSA) 还原密钥。
/// 私钥为 PKCS#8(``PRIVATE KEY``), 公钥为 SPKI(``PUBLIC KEY``), 与 dev 测试套件密钥形态一致(契约 §二)。
public final class MockPemUtil {

    private MockPemUtil() {
    }

    /// 加载 PKCS#8 私钥(PEM 原文, 商户私钥请求签名用)
    public static PrivateKey loadPrivateKey(String pem) {
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decodePem(pem)));
        } catch (Exception e) {
            throw new IllegalStateException("RSA 私钥 PEM 解析失败", e);
        }
    }

    /// 加载 SPKI 公钥(PEM 原文, 平台公钥验签用)
    public static PublicKey loadPublicKey(String pem) {
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decodePem(pem)));
        } catch (Exception e) {
            throw new IllegalStateException("RSA 公钥 PEM 解析失败", e);
        }
    }

    /// 剥离 PEM 头尾行与全部空白后 Base64 解码
    private static byte[] decodePem(String pem) {
        String body = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
