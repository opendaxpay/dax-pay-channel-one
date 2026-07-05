package cn.daxpay.open.channel.lakala.sdk;

import cn.daxpay.open.channel.lakala.code.LakalaCode;
import cn.daxpay.open.channel.lakala.config.LakalaSdkCredential;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// # 拉卡拉 API 客户端
///
/// 封装拉卡拉开放平台 V3 接口调用: 组装请求体(req_time/version/req_data) → RSA2 签名 → HTTP POST → 响应验签 → 业务码校验。
/// 交易类接口统一返回 `resp_data` JSON 对象; 成功码为 `BBS00000` / `000000`。
///
/// @link <a href="https://o.lakala.com/">拉卡拉开放平台</a>
@Slf4j
public final class LakalaClient {

    /// 成功响应码白名单
    private static final List<String> SUCCESS_CODES = List.of("BBS00000", "000000");

    private LakalaClient() {
    }

    /// 发送交易类请求
    ///
    /// @param credential 通道凭证(含服务商密钥 + 商户号/终端号 + 沙箱标识)
    /// @param bizParam   业务参数对象(序列化为 req_data)
    /// @param path       API 路径(如 /v3/labs/trans/preorder)
    /// @return resp_data JSON 对象
    public static JSONObject tradePost(LakalaSdkCredential credential, Object bizParam, String path) {
        return doPost(credential, bizParam, path, "req_data", "resp_data", SUCCESS_CODES, "code", "msg");
    }

    /// 通用请求发送(支持自定义外层字段名与成功码)
    @SuppressWarnings("SameParameterValue")
    private static JSONObject doPost(LakalaSdkCredential credential, Object bizParam, String path,
                                     String reqDataKey, String respDataKey,
                                     List<String> successCodes, String codeKey, String msgKey) {
        // 组装外层请求体
        Map<String, Object> param = new HashMap<>();
        param.put("req_time", LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN));
        param.put("version", LakalaCode.VERSION);
        param.put(reqDataKey, bizParam);
        String json = JSONUtil.toJsonStr(param);

        // 网关地址(沙箱/生产)
        String apiUrl = Boolean.TRUE.equals(credential.getSandbox())
                ? LakalaCode.SANDBOX_SERVER_URL
                : LakalaCode.PRODUCTION_SERVER_URL;

        // 生成签名
        String authorization = LakalaSignUtil.generateSign(
                credential.getLklAppId(), credential.getMchSerialNo(), credential.getPrivateKey(), json);

        // 发送 HTTP POST
        var response = HttpUtil.createPost(apiUrl + path)
                .body(json)
                .header(LakalaCode.HEADER_AUTHORIZATION, authorization)
                .header(LakalaCode.HEADER_LKL_OP_SDK, LakalaCode.LKL_OP_SDK)
                .header(LakalaCode.HEADER_LKL_OP_FLOWGROUP, LakalaCode.LKL_OP_FLOWGROUP)
                .header(LakalaCode.HEADER_LKL_OP_APPID, credential.getLklAppId())
                .timeout(15000)
                .execute();
        String body = response.body();
        log.info("拉卡拉请求 path={}, outTradeNo={}", path, extractOutTradeNo(bizParam));
        log.debug("拉卡拉请求体: {}", json);
        log.debug("拉卡拉响应体: {}", body);

        // 收集响应头(小写 key, 验签用)
        Map<String, String> headers = new HashMap<>();
        response.headers().keySet().stream()
                .filter(Objects::nonNull)
                .forEach(k -> headers.put(k.toLowerCase(), response.headers().get(k).getFirst()));

        // 验签
        if (!LakalaSignUtil.verifySign(headers, body, credential.getPublicKey())) {
            log.error("拉卡拉响应验签失败: path={}", path);
            throw new ChannelServiceException(ChannelErrorCode.CALLBACK_VERIFY_FAILED);
        }

        // 业务码校验
        var jsonObject = JSONUtil.parseObj(body);
        String code = jsonObject.getStr(codeKey);
        if (!successCodes.contains(code)) {
            String msg = jsonObject.getStr(msgKey);
            log.error("拉卡拉接口异常: path={}, code={}, msg={}", path, code, msg);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.lakalaPayFailed", StrUtil.isBlank(msg) ? code : msg);
        }
        return jsonObject.getJSONObject(respDataKey);
    }

    /// 从业务参数中提取 outTradeNo(仅用于日志)
    private static String extractOutTradeNo(Object bizParam) {
        if (bizParam == null) {
            return "";
        }
        try {
            JSONObject json = JSONUtil.parseObj(JSONUtil.toJsonStr(bizParam));
            return json.getStr("out_trade_no");
        } catch (Exception e) {
            return "";
        }
    }
}
