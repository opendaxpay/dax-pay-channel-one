package cn.daxpay.open.channel.union.util;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Objects;

/// # 云闪付 RSA2 证书签名工具
///
/// 银联 ACP(全渠道支付)采用 RSA2 证书签名(区别于银联商务的 HmacSHA256 无证书模式):
/// - **请求签名**: 私钥证书(PKCS12) + SHA256withRSA, 同时上报签名证书序列号(certId)
/// - **回调验签**: 银联回调附带 signPubKeyCert, 用其公钥校验 signature
///
/// ACP 5.1 签名规则:
/// 1. 报文字段按 key 的 ASCII 字典序排序, 拼成 `key1=value1&key2=value2&...`
/// 2. 排除 signature 字段与空值
/// 3. 对待签串做 SHA-256, 转为小写十六进制摘要串
/// 4. 使用 SHA256withRSA 对摘要串签名, 最后 Base64 编码
///
/// 实现基于 JDK 原生 java.security, 无第三方加密库依赖。
@Slf4j
@UtilityClass
public class UnionSignUtil {

    /// 签名算法(银联 ACP 固定 SHA256withRSA)
    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    /// 摘要算法
    private static final String DIGEST_ALGORITHM = "SHA-256";

    /// PKCS12 证书库类型
    private static final String KEYSTORE_TYPE = "PKCS12";

    /// X.509 证书工厂类型
    private static final String CERT_TYPE = "X.509";

    /// 获取私钥签名证书序列号(银联 certId)
    public String getSignCertId(UnionSdkCredential cred) {
        X509Certificate cert = loadSignCert(cred);
        return cert.getSerialNumber().toString();
    }

    /// 对报文参数进行 ACP 5.1 RSA2 签名, 返回 Base64 签名串
    public String sign(Map<String, ?> params, UnionSdkCredential cred) {
        String digestHex = sha256Hex(buildSignString(params));
        PrivateKey privateKey = loadPrivateKey(cred);
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(digestHex.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            log.error("云闪付签名异常", e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.unionSignFailed", e.getMessage());
        }
    }

    /// 验证银联异步回调签名
    public boolean verifyCallback(Map<String, ?> params, UnionSdkCredential cred) {
        Object signatureValue = params.get("signature");
        Object certValue = params.get("signPubKeyCert");
        String sign = signatureValue == null ? null : String.valueOf(signatureValue);
        String signPubKeyCert = certValue == null ? null : String.valueOf(certValue);
        if (StrUtil.isBlank(sign) || StrUtil.isBlank(signPubKeyCert)) {
            log.error("云闪付回调参数缺少 signature 或 signPubKeyCert");
            return false;
        }
        try {
            X509Certificate cert = parseSignPubKeyCert(signPubKeyCert);
            PublicKey publicKey = cert.getPublicKey();
            String digestHex = sha256Hex(buildSignString(params));
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(digestHex.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.error("验证云闪付回调签名异常", e);
            return false;
        }
    }

    /// 构建待签名字符串(过滤空值、排除 signature、按 key 字典序)
    public String buildSignString(Map<String, ?> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> Objects.nonNull(e.getValue()) && !"".equals(e.getValue()))
                .filter(e -> !"signature".equals(e.getKey()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    /// SHA-256 摘要并转小写十六进制串。
    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.unionSignFailed", e.getMessage());
        }
    }

    /// 银联同步/异步响应中的 signPubKeyCert 可能是 PEM，也可能是纯 Base64 DER。
    private X509Certificate parseSignPubKeyCert(String certText) throws Exception {
        String normalized = certText
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] certBytes = Base64.getDecoder().decode(normalized);
        CertificateFactory cf = CertificateFactory.getInstance(CERT_TYPE);
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    /// 加载私钥证书(PKCS12)中的 PrivateKey
    private PrivateKey loadPrivateKey(UnionSdkCredential cred) {
        try {
            KeyStore ks = loadKeyStore(cred);
            String alias = ks.aliases().nextElement();
            return (PrivateKey) ks.getKey(alias, cred.getKeyPrivateCertPwd().toCharArray());
        } catch (Exception e) {
            log.error("加载云闪付私钥证书失败", e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.unionCertLoadFailed", e.getMessage());
        }
    }

    /// 加载私钥证书(PKCS12)中的签名证书 X509Certificate
    private X509Certificate loadSignCert(UnionSdkCredential cred) {
        try {
            KeyStore ks = loadKeyStore(cred);
            String alias = ks.aliases().nextElement();
            return (X509Certificate) ks.getCertificate(alias);
        } catch (Exception e) {
            log.error("加载云闪付签名证书失败", e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.unionCertLoadFailed", e.getMessage());
        }
    }

    /// 解析 PKCS12 证书库(Base64 字符串 → KeyStore)
    private KeyStore loadKeyStore(UnionSdkCredential cred) throws Exception {
        byte[] pfxBytes = Base64.getDecoder().decode(cred.getKeyPrivateCert());
        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
        ks.load(new ByteArrayInputStream(pfxBytes), cred.getKeyPrivateCertPwd().toCharArray());
        return ks;
    }
}
