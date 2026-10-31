package cn.daxpay.open.channel.mock.sdk;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/// # Mock 网关验签器
///
/// 三行验签串(与请求签名串同构, 经 [MockRequestSigner#buildSignSource] 构造):
/// ``{timestamp}\n{nonce}\n{报文原文}\n``
/// 网关响应验签(含 4xx/5xx)与回调验签共用本类, 签名者均为平台角色(10400 模拟平台服务器),
/// 统一用凭证下发的平台公钥校验; 签名头缺失/签名不匹配均按验签失败处理。
public final class MockSignVerifier {

    private MockSignVerifier() {
    }

    /// 平台公钥验签: 三行验签串 RSA-SHA256 校验
    ///
    /// @param platformKey 平台公钥(凭证下发)
    /// @param signB64     签名值(Mock-Sign 头/回调 sign 字段, Base64)
    /// @param timestamp   秒级时间戳(Mock-Timestamp 头/回调 timestamp 字段)
    /// @param nonce       32 位随机 hex(Mock-Nonce 头/回调 nonce 字段)
    /// @param body        报文 JSON 原文(响应体/回调 body)
    public static void verify(PublicKey platformKey, String signB64,
                              String timestamp, String nonce, String body) {
        if (signB64 == null || timestamp == null || nonce == null) {
            throw new IllegalArgumentException("签名头缺失");
        }
        String source = MockRequestSigner.buildSignSource(timestamp, nonce, body);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(platformKey);
            signature.update(source.getBytes(StandardCharsets.UTF_8));
            if (!signature.verify(Base64.getDecoder().decode(signB64))) {
                throw new IllegalArgumentException("签名不匹配");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("签名验证异常: " + e.getMessage(), e);
        }
    }
}
