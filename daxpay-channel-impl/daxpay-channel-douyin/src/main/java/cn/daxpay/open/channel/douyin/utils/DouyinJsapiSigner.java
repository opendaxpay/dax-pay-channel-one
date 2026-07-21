package cn.daxpay.open.channel.douyin.utils;

import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/// # 抖音 JSAPI 调起签名工具
///
/// 抖音 H5 JSAPI 调起支付(ttcjpay.dypay)所需的 paySign 计算:
/// 签名串固定 4 行, 每行以 \n 结尾(含末行):
/// ```
/// appId
/// timeStamp
/// nonceStr
/// prepay_id=xxx
/// ```
/// 算法: SHA256withRSA + Base64, 私钥为 PKCS8 base64 字符串(与抖音支付 SDK 同一把)。
///
/// 参考文档: https://pay.douyinpay.com/wiki/639fd48f17c2f3021d237f61/64413fddc6217f024ae23ae9.md
public final class DouyinJsapiSigner {

    private DouyinJsapiSigner() {
    }

    /// 签名算法
    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    /// 密钥算法
    private static final String KEY_ALGORITHM = "RSA";

    /// 计算 JSAPI 调起支付的 paySign
    ///
    /// @param appId        抖音应用 appId(等同开放平台 client_key)
    /// @param timeStamp    时间戳(秒, 字符串)
    /// @param nonceStr     随机字符串
    /// @param prepayId     JSAPI 下单返回的 prepayId
    /// @param privateKeyPkcs8Base64 商户私钥(PKCS8 base64 字符串, 与抖音支付 SDK 同一把)
    /// @return Base64 编码的签名值
    public static String signPayInfo(String appId, String timeStamp, String nonceStr,
                                     String prepayId, String privateKeyPkcs8Base64) {
        if (StrUtil.hasBlank(appId, timeStamp, nonceStr, prepayId, privateKeyPkcs8Base64)) {
            throw new SdkCallException("抖音 JSAPI 签名参数存在空值");
        }
        String packageValue = "prepay_id=" + prepayId;
        // 4 行签名串, 每行末尾以 \n 结尾(含末行)
        String signStr = String.join("\n",
                appId,
                timeStamp,
                nonceStr,
                packageValue) + "\n";
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyPkcs8Base64);
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(signStr.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new SdkCallException("抖音 JSAPI paySign 签名失败: " + e.getMessage());
        }
    }

    /// 加载 PKCS8 私钥字符串
    ///
    /// 兼容两种输入:
    /// 1. 纯 base64 字符串
    /// 2. PEM 格式(`-----BEGIN PRIVATE KEY-----` 包裹, 含换行)
    ///
    /// 与抖音 SDK `com.douyinpay.util.PemUtil#loadPrivateKeyBase64` 行为一致,
    /// 先剥离 PEM header / 换行 / 空白, 否则 `Base64.getDecoder()` 遇到 `-` 会抛 Illegal base64 character 2d
    private static PrivateKey loadPrivateKey(String pkcs8Base64) throws Exception {
        String normalized = pkcs8Base64
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }
}
