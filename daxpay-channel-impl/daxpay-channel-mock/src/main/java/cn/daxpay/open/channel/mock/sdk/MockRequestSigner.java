package cn.daxpay.open.channel.mock.sdk;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;

/// # Mock 网关请求签名器
///
/// DaxPay 开放接口风格签名(三行签名串 + 三个签名头), 商户私钥 RSA-SHA256 签名(纯 JDK Signature 实现)。
///
/// 三行签名串(每段以 ``\n`` 结尾):
/// ``{timestamp}\n{nonce}\n{body}\n``
/// 其中 body 为请求 JSON 原文(GET/DELETE 无请求体时为空串); 签名不覆盖 HTTP 方法与 URL,
/// query 参数仅随 URL 原样携带。
public final class MockRequestSigner {

    private static final SecureRandom RANDOM = new SecureRandom();

    private MockRequestSigner() {
    }

    /// 构造三行签名串(请求签名与响应/回调验签共用同一形态)
    public static String buildSignSource(String timestamp, String nonceStr, String body) {
        return timestamp + "\n" + nonceStr + "\n" + body + "\n";
    }

    /// RSA-SHA256 签名, 返回 Base64
    public static String sign(PrivateKey privateKey, String source) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(source.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Mock 网关请求签名失败", e);
        }
    }

    /// 生成 32 位随机 hex nonce(16 字节随机数)
    public static String nonceStr() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
