package cn.daxpay.open.channel.douyin.utils;

import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.daxpay.open.platform.core.util.RsaKeyLoader;
import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
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
/// 算法: SHA256withRSA + Base64, 私钥 PKCS#8/PKCS#1 均支持(与抖音支付 SDK 同一把), PEM 头尾与换行可选。
///
/// 参考文档: https://pay.douyinpay.com/wiki/639fd48f17c2f3021d237f61/64413fddc6217f024ae23ae9.md
public final class DouyinJsapiSigner {

    private DouyinJsapiSigner() {
    }

    /// 签名算法
    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    /// 计算 JSAPI 调起支付的 paySign
    ///
    /// @param appId        抖音应用 appId(等同开放平台 client_key)
    /// @param timeStamp    时间戳(秒, 字符串)
    /// @param nonceStr     随机字符串
    /// @param prepayId     JSAPI 下单返回的 prepayId
    /// @param privateKeyPkcs8Base64 商户私钥(PKCS#8/PKCS#1 均可, 与抖音支付 SDK 同一把)
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

    /// 加载私钥(PKCS#8 优先, 回退 PKCS#1; 兼容 PEM 头尾与换行)
    private static PrivateKey loadPrivateKey(String privateKeyContent) throws Exception {
        return RsaKeyLoader.loadPrivateKey(privateKeyContent);
    }
}
