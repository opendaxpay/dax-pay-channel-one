package cn.daxpay.open.channel.mock.sdk;

import cn.daxpay.open.channel.mock.config.MockGatewayProperties;
import cn.daxpay.open.channel.mock.config.MockSdkCredential;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/// # Mock 网关客户端(模拟微信官方 SDK 角色)
///
/// 调用 10400 mock 网关(模拟微信开放平台服务器)的 ``/v3/mock/**`` 端点, 协议为 DaxPay 开放接口风格:
/// - 每请求带三个签名头(Mock-Sign/Mock-Timestamp/Mock-Nonce), 商户私钥对三行签名串
///   ``{timestamp}\n{nonce}\n{body}\n`` 做 RSA-SHA256 签名(GET/DELETE 请求体为空串);
/// - 密钥主体编号(可选, 仅主体级独立密钥时携带)随请求报文/查询串以 ``key_owner_no`` 字段传递, 不额外占用请求头:
///   POST 走报文字段(在构造签名串之前合并, 该字段参与签名), GET/DELETE 走查询串(不进签名串);
///   携带时 10400 按主体编号查公钥登记表验签(服务商查 isv_no, 直连查 channel_mch_no),
///   未携带回落全局公钥(直连/全局密钥现状不变);
/// - 每响应强制验签(平台公钥 + 同名三头 + 三行验签串, 含 4xx/5xx), 失败按网络层错误处理
///   (DaxResult code 10003, msg 注明"网关响应验签失败");
/// - 非 2xx 解析 body 的 internal_code/message: internal_code 透传为主 code, 无则 10003, msg 取 message
///   (业务错误码一律按网关 internal_code 透传, 不再单列特殊分支)。
///
/// 凭证随请求下发(one 无状态), 网关基地址来自 [MockGatewayProperties];
/// HTTP 传输使用全局共享 RestClient(由启动模块 RestClientConfiguration 提供, 底层 HC5 连接池)。
@Slf4j
@Component
public class MockGatewayClient {

    /// 商户号缺省值(内部 DTO 未携带商户标识的端点使用; 网关侧单租户, 仅业务报文与留痕用途)
    public static final String DEFAULT_MCHID = "MOCK";

    /// 签名三头(请求与响应同名, 响应含 4xx/5xx)
    private static final String HEADER_SIGN = "Mock-Sign";
    private static final String HEADER_TIMESTAMP = "Mock-Timestamp";
    private static final String HEADER_NONCE = "Mock-Nonce";

    /// 密钥主体编号字段名(随请求报文/查询串携带, 10400 按主体编号查公钥登记表验签)
    private static final String FIELD_KEY_OWNER_NO = "key_owner_no";

    private final RestClient restClient;

    private final String baseUrl;

    public MockGatewayClient(RestClient restClient, MockGatewayProperties properties) {
        this.restClient = restClient;
        this.baseUrl = StrUtil.removeSuffix(properties.getBaseUrl(), "/");
    }

    /// 解析网关商户号 mchid: 主应用商户号优先, 通道商户号兜底, 均无取缺省常量(业务报文 mchid↔mchNo)
    public static String resolveMchid(String mchNo, String channelMchNo) {
        if (StrUtil.isNotBlank(mchNo)) {
            return mchNo;
        }
        if (StrUtil.isNotBlank(channelMchNo)) {
            return channelMchNo;
        }
        return DEFAULT_MCHID;
    }

    /// POST JSON 网关调用(返回解析后的 snake_case 响应)
    public JSONObject post(MockSdkCredential credential, String mchid, String path, Map<String, Object> body) {
        // 主体编号合并进待序列化报文: 必须在构造签名串之前, 该字段随 POST 报文一起被签名覆盖
        Map<String, Object> payload = mergeKeyOwnerNo(credential, body);
        String bodyStr = payload == null ? "" : JSONUtil.toJsonStr(payload);
        return this.exchange(credential, mchid, HttpMethod.POST, path, Map.of(), bodyStr, true);
    }

    /// POST JSON 网关调用(无业务响应体, 仅校验签名与状态码, 如分账接收方绑定/解绑)
    public void postForVoid(MockSdkCredential credential, String mchid, String path, Map<String, Object> body) {
        // 同 post: 主体编号先并入报文再序列化签名
        Map<String, Object> payload = mergeKeyOwnerNo(credential, body);
        String bodyStr = payload == null ? "" : JSONUtil.toJsonStr(payload);
        this.exchange(credential, mchid, HttpMethod.POST, path, Map.of(), bodyStr, false);
    }

    /// GET 网关调用(query 拼入请求 URL, 主体编号随查询串携带, 不进签名串)
    public JSONObject get(MockSdkCredential credential, String mchid, String path, Map<String, String> query) {
        return this.exchange(credential, mchid, HttpMethod.GET, path,
                mergeKeyOwnerQuery(credential, query), "", true);
    }

    /// DELETE 网关调用(无请求体, 如终端注销; 主体编号随查询串携带, 不进签名串)
    public void delete(MockSdkCredential credential, String mchid, String path, Map<String, String> query) {
        this.exchange(credential, mchid, HttpMethod.DELETE, path,
                mergeKeyOwnerQuery(credential, query), "", false);
    }

    /// 将密钥主体编号合并进待发送报文: 仅主体编号非空时注入; 调用方大量使用 Map.of 等不可变集合,
    /// 故始终复制新 Map 再合并, 不改动调用方报文(无主体编号时原样返回, 回落全局公钥)
    private static Map<String, Object> mergeKeyOwnerNo(MockSdkCredential credential, Map<String, Object> body) {
        String keyOwnerNo = credential.getKeyOwnerNo();
        if (StrUtil.isBlank(keyOwnerNo)) {
            return body;
        }
        Map<String, Object> merged = body == null ? new LinkedHashMap<>() : new LinkedHashMap<>(body);
        merged.put(FIELD_KEY_OWNER_NO, keyOwnerNo);
        return merged;
    }

    /// 将密钥主体编号合并进查询参数: 仅主体编号非空时注入; 复制新 Map 避免改动调用方
    /// Map.of/HashMap 实例(无主体编号时原样返回, 回落全局公钥)
    private static Map<String, String> mergeKeyOwnerQuery(MockSdkCredential credential, Map<String, String> query) {
        String keyOwnerNo = credential.getKeyOwnerNo();
        if (StrUtil.isBlank(keyOwnerNo)) {
            return query;
        }
        Map<String, String> merged = query == null ? new LinkedHashMap<>() : new LinkedHashMap<>(query);
        merged.put(FIELD_KEY_OWNER_NO, keyOwnerNo);
        return merged;
    }

    /// 通用网关调用: 签名 → 发送 → 验签 → 状态码/业务错误映射
    private JSONObject exchange(MockSdkCredential credential, String mchid, HttpMethod method, String path,
                                Map<String, String> query, String body, boolean parseJson) {
        String pathWithQuery = buildPathWithQuery(path, query);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = MockRequestSigner.nonceStr();
        String sign = MockRequestSigner.sign(MockPemUtil.loadPrivateKey(credential.getMerchantPrivateKey()),
                MockRequestSigner.buildSignSource(timestamp, nonceStr, body));
        // 服务商接入网关地址优先(行内配置, 可空), 空回落 yml base-url(直连现状)
        String base = StrUtil.isNotBlank(credential.getGatewayBaseUrl())
                ? StrUtil.removeSuffix(credential.getGatewayBaseUrl(), "/")
                : this.baseUrl;
        URI uri = URI.create(base + pathWithQuery);
        log.debug("Mock 网关请求: mchid={}, {} {}", mchid, method, pathWithQuery);
        GatewayRawResponse raw = this.doExecute(method, uri, timestamp, nonceStr, sign, body);
        // 响应强制验签: 头缺失/签名不匹配均按网络层错误(10003)处理
        try {
            MockSignVerifier.verify(MockPemUtil.loadPublicKey(credential.getPlatformPublicKey()),
                    raw.sign(), raw.timestamp(), raw.nonce(), raw.body());
        } catch (Exception e) {
            log.error("Mock 网关响应验签失败: path={}, 原因={}", pathWithQuery, e.getMessage());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "mock.error.gatewayResponseVerifyFailed", e.getMessage());
        }
        if (raw.statusCode() < 200 || raw.statusCode() >= 300) {
            throwBizError(raw.body(), raw.statusCode());
        }
        if (!parseJson || StrUtil.isBlank(raw.body())) {
            return null;
        }
        return JSONUtil.parseObj(raw.body());
    }

    /// 执行 HTTP 请求, 携带签名三头, 捕获状态码/响应签名三头/响应体原文
    private GatewayRawResponse doExecute(HttpMethod method, URI uri, String timestamp, String nonceStr,
                                         String sign, String body) {
        try {
            RestClient.RequestBodySpec spec = this.restClient.method(method)
                    .uri(uri)
                    .header(HEADER_SIGN, sign)
                    .header(HEADER_TIMESTAMP, timestamp)
                    .header(HEADER_NONCE, nonceStr)
                    .contentType(MediaType.APPLICATION_JSON);
            if (!body.isEmpty()) {
                spec.body(body);
            }
            return spec.exchange((request, response) -> {
                HttpHeaders headers = response.getHeaders();
                String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                return new GatewayRawResponse(response.getStatusCode().value(),
                        headers.getFirst(HEADER_SIGN), headers.getFirst(HEADER_TIMESTAMP),
                        headers.getFirst(HEADER_NONCE), responseBody);
            });
        } catch (ChannelServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Mock 网关请求异常: uri={}", uri, e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "mock.error.gatewayRequestFailed", e.getMessage());
        }
    }

    /// 非 2xx 业务错误映射: internal_code 透传为主 code, 无则 10003; msg 取网关 message
    private void throwBizError(String body, int statusCode) {
        int code = ChannelErrorCode.SDK_CALL_FAILED.getCode();
        String message = "Mock 网关调用失败(HTTP " + statusCode + ")";
        if (StrUtil.isNotBlank(body)) {
            try {
                JSONObject json = JSONUtil.parseObj(body);
                String internalCode = json.getStr("internal_code");
                if (StrUtil.isNotBlank(internalCode)) {
                    code = Integer.parseInt(internalCode);
                }
                message = StrUtil.blankToDefault(json.getStr("message"), message);
            } catch (Exception ignore) {
                // 非 JSON 错误体(代理错误等), 维持默认文案
            }
        }
        // 网关报文自带文案直传: 非 i18n key 时 I18nUtil 原样返回(不经过 MessageFormat, 避免引号被吞)
        throw new ChannelServiceException(code, message);
    }

    /// 构造请求 URL 的 path+query(query 按 key 字典序, 值不做 URL encode, 保证请求 URL 稳定可读)
    private static String buildPathWithQuery(String path, Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return path;
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (entry.getValue() != null) {
                sorted.put(entry.getKey(), entry.getValue());
            }
        }
        if (sorted.isEmpty()) {
            return path;
        }
        StringBuilder sb = new StringBuilder(path).append('?');
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    /// 网关原始响应(状态码 + 签名三头 + body 原文)
    private record GatewayRawResponse(int statusCode, String sign, String timestamp,
                                      String nonce, String body) {
    }
}
