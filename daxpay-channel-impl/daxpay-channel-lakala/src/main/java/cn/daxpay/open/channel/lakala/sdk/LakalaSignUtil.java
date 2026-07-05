package cn.daxpay.open.channel.lakala.sdk;

import cn.daxpay.open.channel.lakala.code.LakalaCode;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/// # 拉卡拉 V3 签名工具
///
/// 实现拉卡拉安全统一接入规范的签名/验签:
/// - 请求签名: SHA256withRSA, Authorization = `LKLAPI-SHA256withRSA appid="...",nonce_str="...",timestamp="...",serial_no="...",signature="..."`
/// - 响应验签: 从响应头 Lklapi-* 读取签名要素, 用拉卡拉公钥证书验证
///
/// @link <a href="https://o.lakala.com/#/home/document/detail?id=33">拉卡拉安全统一接入规范</a>
@Slf4j
public final class LakalaSignUtil {

    private LakalaSignUtil() {
    }

    /// 生成请求签名(Authorization 头值)
    ///
    /// @param lklAppId   拉卡拉应用编号
    /// @param mchSerialNo 商户证书序列号
    /// @param privateKey  商户RSA私钥(PEM PKCS#8)
    /// @param jsonBody    请求体 JSON 字符串
    /// @return Authorization 头完整值
    public static String generateSign(String lklAppId, String mchSerialNo, String privateKey, String jsonBody) {
        String nonceStr = RandomUtil.randomString(12);
        long timestamp = System.currentTimeMillis() / 1000;
        // 签名原文: appid\n serial_no\n timestamp\n nonce_str\n body\n
        String message = lklAppId + "\n" + mchSerialNo + "\n" + timestamp + "\n" + nonceStr + "\n" + jsonBody + "\n";
        try {
            String signature = doSign(message.getBytes(StandardCharsets.UTF_8), loadPrivateKey(privateKey));
            return LakalaCode.AUTH_SCHEME
                    + " appid=\"" + lklAppId + "\","
                    + "nonce_str=\"" + nonceStr + "\","
                    + "timestamp=\"" + timestamp + "\","
                    + "serial_no=\"" + mchSerialNo + "\","
                    + "signature=\"" + signature + "\"";
        } catch (Exception e) {
            log.error("拉卡拉签名计算失败: lklAppId={}", lklAppId, e);
            throw new SdkCallException("拉卡拉签名计算失败: " + e.getMessage(), e);
        }
    }

    /// 验证拉卡拉响应签名
    ///
    /// @param headers    响应头(全小写 key)
    /// @param body       响应体原文
    /// @param publicKey  拉卡拉公钥证书(PEM)
    /// @return 验签是否通过
    public static boolean verifySign(Map<String, String> headers, String body, String publicKey) {
        String appid = headers.getOrDefault("lklapi-appid", "");
        String serial = headers.getOrDefault("lklapi-serial", "");
        String timestamp = headers.getOrDefault("lklapi-timestamp", "");
        String nonce = headers.getOrDefault("lklapi-nonce", "");
        String signature = headers.getOrDefault("lklapi-signature", "");
        if (signature.isEmpty()) {
            return false;
        }
        String source = appid + "\n" + serial + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        try {
            X509Certificate cert = loadCertificate(publicKey);
            return doVerify(cert, source.getBytes(StandardCharsets.UTF_8), signature);
        } catch (Exception e) {
            log.error("拉卡拉验签异常", e);
            throw new ChannelServiceException(ChannelErrorCode.CALLBACK_VERIFY_FAILED);
        }
    }

    /// 加载 PKCS#8 PEM 私钥
    private static PrivateKey loadPrivateKey(String privateKey) throws Exception {
        String key = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    /// 加载 X509 公钥证书(PEM)
    private static X509Certificate loadCertificate(String publicKey) throws Exception {
        String key = publicKey
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        return (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(decoded));
    }

    /// SHA256withRSA 签名
    private static String doSign(byte[] message, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message);
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /// SHA256withRSA 验签
    private static boolean doVerify(X509Certificate cert, byte[] message, String signature) throws Exception {
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(cert);
        sign.update(message);
        return sign.verify(Base64.getDecoder().decode(signature));
    }
}
