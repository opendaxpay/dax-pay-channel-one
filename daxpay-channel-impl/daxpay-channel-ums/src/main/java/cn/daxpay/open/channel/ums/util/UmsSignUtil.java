package cn.daxpay.open.channel.ums.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/// # 银联商务签名工具
///
/// 银联商务不使用证书, 仅依赖两类密钥:
/// - **appKey**: 用于请求签名(HmacSHA256), 覆盖扫码(OPEN-BODY-SIG)与 H5(OPEN-FORM-PARAM)
/// - **secretKey**: 用于异步回调验签(MD5 / SHA256 字典序拼接)
///
/// 签名算法来源: 银联商务开放平台《HTTP 报文签名规则》, 使用 JDK 原生实现(无 commons-codec 依赖)。
@Slf4j
@UtilityClass
public class UmsSignUtil {

    /// 生成 OPEN-BODY-SIG 方式的 Authorization 头(扫码等 POST JSON 接口)
    ///
    /// 签名步骤:
    /// 1. body 做 SHA256 得 hex 摘要
    /// 2. 拼接 `appId + timestamp + nonce + bodyDigest`
    /// 3. 对拼接串做 HmacSHA256(appKey) → Base64
    /// 4. 组装完整 Authorization 字符串
    ///
    /// @param appId  银联商务应用 AppId
    /// @param appKey 应用密钥(HmacSHA256 密钥)
    /// @param body   请求体 JSON 字符串
    /// @return 完整 Authorization 头值
    public String getOpenBodySig(String appId, String appKey, String body) {
        String timestamp = UmsDateUtil.h5Timestamp();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String bodyDigest = sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        String signContent = appId + timestamp + nonce + bodyDigest;
        byte[] signature = hmacSha256(signContent.getBytes(StandardCharsets.UTF_8),
                appKey.getBytes(StandardCharsets.UTF_8));
        String signatureStr = Base64.getEncoder().encodeToString(signature);
        // 组装格式: OPEN-BODY-SIG AppId="...", Timestamp="...", Nonce="...", Signature="..."
        return "OPEN-BODY-SIG AppId=\"" + appId + "\", Timestamp=\"" + timestamp
                + "\", Nonce=\"" + nonce + "\", Signature=\"" + signatureStr + "\"";
    }

    /// 生成 H5 接口签名(OPEN-FORM-PARAM 方式的 signature 参数值)
    ///
    /// 算法与 [getOpenBodySig] 相同, 但 timestamp/nonce 由调用方传入(H5 需拼到 URL 中保持一致),
    /// 返回纯 Base64 签名串(非完整 Authorization)。
    ///
    /// @param appId     银联商务应用 AppId
    /// @param appKey    应用密钥
    /// @param timestamp 时间戳(yyyyMMddHHmmss)
    /// @param nonce     随机字符串
    /// @param body      请求体 JSON 字符串
    /// @return Base64 签名串
    public String getSignature(String appId, String appKey, String timestamp, String nonce, String body) {
        String bodyDigest = sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        String signContent = appId + timestamp + nonce + bodyDigest;
        byte[] signature = hmacSha256(signContent.getBytes(StandardCharsets.UTF_8),
                appKey.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }

    /// 验证异步回调签名
    ///
    /// 银联商务回调为 form 参数(Map), 验签步骤:
    /// 1. 取 `sign` 与 `signType`(MD5 / SHA256)
    /// 2. 过滤空值、排除 `sign` 字段, 按字段名字典序拼成 `key1=value1&key2=value2`
    /// 3. 追加 `secretKey` 后做 MD5 或 SHA256
    /// 4. 与回调中的 sign 比对(大小写不敏感)
    ///
    /// @param callbackParam 回调参数(字段名→字段值)
    /// @param secretKey     通讯密钥
    /// @return 验签是否通过
    public boolean verifyCallback(Map<String, String> callbackParam, String secretKey) {
        try {
            String sign = callbackParam.get("sign");
            String signType = callbackParam.get("signType");
            if (StrUtil.isBlank(sign)) {
                log.error("银联商务回调参数缺少 sign 字段");
                return false;
            }
            String dataToSign = buildSignString(callbackParam);
            String calculatedSign;
            if ("MD5".equals(signType)) {
                calculatedSign = md5Hex(dataToSign + secretKey);
            } else {
                // 默认 SHA256
                calculatedSign = sha256Hex((dataToSign + secretKey).getBytes(StandardCharsets.UTF_8));
            }
            return sign.equalsIgnoreCase(calculatedSign);
        } catch (Exception e) {
            log.error("验证银联商务回调签名异常", e);
            return false;
        }
    }

    /// 构建 H5 跳转链接(OPEN-FORM-PARAM 方式)
    ///
    /// 将 appId / timestamp / nonce / content(请求体 URL 编码) / signature(签名 URL 编码) 拼到 URL query。
    ///
    /// @param url       银联商务接口地址
    /// @param appId     应用 AppId
    /// @param timestamp 时间戳
    /// @param nonce     随机串
    /// @param reqBody   请求体 JSON
    /// @param signature 签名串([getSignature] 返回值)
    /// @return 完整跳转链接
    public String buildH5Url(String url, String appId, String timestamp, String nonce,
                             String reqBody, String signature) {
        return MessageFormat.format(
                "{0}?authorization=OPEN-FORM-PARAM&appId={1}&timestamp={2}&nonce={3}&content={4}&signature={5}",
                url, appId, timestamp, nonce,
                URLEncoder.encode(reqBody, StandardCharsets.UTF_8),
                URLEncoder.encode(signature, StandardCharsets.UTF_8));
    }

    /// SHA256 → 小写 hex 字符串
    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexUtil.encodeHexStr(md.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("SHA256 计算失败", e);
        }
    }

    /// MD5 → 小写 hex 字符串
    private String md5Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexUtil.encodeHexStr(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("MD5 计算失败", e);
        }
    }

    /// HmacSHA256 签名
    private byte[] hmacSha256(byte[] data, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HmacSHA256 计算失败", e);
        }
    }

    /// 构建回调待签名字符串(过滤空值、排除 sign、字典序)
    private String buildSignString(Map<String, String> params) {
        Map<String, String> filtered = params.entrySet().stream()
                .filter(e -> StrUtil.isNotBlank(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<String> paramList = new TreeSet<>(filtered.keySet()).stream()
                .filter(key -> !"sign".equals(key))
                .map(key -> String.format("%s=%s", key, filtered.get(key)))
                .collect(Collectors.toList());
        return String.join("&", paramList);
    }
}
