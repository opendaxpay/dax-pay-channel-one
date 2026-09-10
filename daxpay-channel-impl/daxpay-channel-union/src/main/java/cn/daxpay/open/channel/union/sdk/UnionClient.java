package cn.daxpay.open.channel.union.sdk;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.util.UnionSignUtil;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/// # 云闪付(直连银联 ACP) HTTP 客户端
///
/// 封装银联全渠道支付平台(95516.com)接口调用:
/// - **后台交易**(POST form, RSA2 证书签名): 主扫申请二维码 / 被扫消费 / 退款 / 关单 / 查询
/// - **前台跳转**(WAP/H5): 返回自动提交的 HTML form, 由用户浏览器 POST 到银联收银台
///
/// 银联 ACP 报文为 form-urlencoded(key=value&...), 非 JSON, 区别于银联商务。
/// 凭证([UnionSdkCredential])随请求下发, 子应用无状态, 每次调用动态构建。
///
/// HTTP 传输使用全局共享的 Spring [RestClient](由 RestClientConfiguration 提供)。
@Slf4j
public class UnionClient {

    /// 沙箱环境主机
    public static final String SANDBOX_HOST = "test.95516.com";

    /// 生产环境主机
    public static final String PRODUCTION_HOST = "95516.com";

    /// 后台交易请求地址(主扫/被扫/退款/关单)
    private static final String BACK_TRANS_URL = "https://gateway.%s/gateway/api/backTransReq.do";

    /// 前台跳转请求地址(H5/WAP 网关支付)
    private static final String FRONT_TRANS_URL = "https://gateway.%s/gateway/api/frontTransReq.do";

    /// 单笔查询请求地址
    private static final String QUERY_TRANS_URL = "https://gateway.%s/gateway/api/queryTrans.do";

    private final UnionSdkCredential credential;

    private final String host;

    private final RestClient restClient;

    /// 根据凭证初始化客户端, 选择沙箱/生产网关
    public UnionClient(UnionSdkCredential credential, RestClient restClient) {
        this.credential = credential;
        this.restClient = restClient;
        this.host = credential.isSandbox() ? SANDBOX_HOST : PRODUCTION_HOST;
    }

    // ===== 后台交易接口(POST form, RSA2 证书签名) =====

    /// 主扫支付(申请二维码, C 扫 B)
    ///
    /// 银联交易类型: txnType=01 / txnSubType=07, 响应中取 qrNo(二维码内容)
    public Map<String, String> applyQrCode(Map<String, Object> param) {
        param.put("txnType", "01");
        param.put("txnSubType", "07");
        param.put("bizType", "000000");
        return tradePost(param, BACK_TRANS_URL);
    }

    /// 被扫支付(付款码消费, B 扫 C)
    ///
    /// 银联交易类型: txnType=01 / txnSubType=06, 需传入 qrNo(用户付款码)
    public Map<String, String> consume(Map<String, Object> param) {
        param.put("txnType", "01");
        param.put("txnSubType", "06");
        param.put("bizType", "000000");
        return tradePost(param, BACK_TRANS_URL);
    }

    /// 退款(退货)
    ///
    /// 银联交易类型: txnType=04 / txnSubType=00, 需传入 origQryId(原交易凭证)
    public Map<String, String> refund(Map<String, Object> param) {
        param.put("txnType", "04");
        param.put("txnSubType", "00");
        param.put("bizType", "000000");
        return tradePost(param, BACK_TRANS_URL);
    }

    /// 关闭订单(交易撤销/关闭)
    ///
    /// 银联交易类型: txnType=31 / txnSubType=00, 需传入 origQryId(原交易凭证)
    public Map<String, String> closeOrder(Map<String, Object> param) {
        param.put("txnType", "31");
        param.put("txnSubType", "00");
        param.put("bizType", "000000");
        return tradePost(param, BACK_TRANS_URL);
    }

    /// 单笔交易查询(支付/退款状态通用)
    public Map<String, String> queryTrans(Map<String, Object> param) {
        return tradePost(param, QUERY_TRANS_URL);
    }

    // ===== 前台跳转接口(H5/WAP 网关支付) =====

    /// 构建 H5/WAP 网关支付自动提交表单
    ///
    /// 银联前台交易必须由浏览器 POST 到 frontTransReq.do(不支持 GET),
    /// 本方法返回一段包含隐藏表单 + 自动 submit 脚本的 HTML,
    /// 主应用前端通过 document.write 或新窗口打开实现跳转。
    ///
    /// 银联交易类型: txnType=01 / txnSubType=01 / bizType=000201
    public String buildWapFormHtml(Map<String, Object> param) {
        param.put("txnType", "01");
        param.put("txnSubType", "01");
        param.put("bizType", "000201");
        signParam(param);
        String url = String.format(FRONT_TRANS_URL, host);
        return buildAutoSubmitForm(url, param);
    }

    // ===== 内部方法 =====

    /// 通用后台 POST 请求(自动签名)
    private Map<String, String> tradePost(Map<String, Object> param, String urlTemplate) {
        signParam(param);
        String url = String.format(urlTemplate, host);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        param.forEach((k, v) -> formData.add(k, Objects.isNull(v) ? "" : String.valueOf(v)));
        String resStr;
        try {
            resStr = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);
        } catch (ChannelServiceException e) {
            throw e;
        } catch (RestClientResponseException e) {
            // 银联业务错误可能以非 2xx 返回, 仍需解析响应体中的 respCode
            resStr = e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("云闪付请求异常: url={}", url, e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.unionRequestFailed", e.getMessage());
        }
        log.info("云闪付请求: url={}, 响应={}", url, resStr);
        Map<String, String> result = parseFormResponse(resStr);
        if (result.isEmpty() || !result.containsKey("respCode")) {
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.unionRequestFailed", StrUtil.isBlank(resStr) ? "空响应" : resStr);
        }
        return result;
    }

    /// 对报文参数补全签名信息(signMethod / certId / signature)
    ///
    /// certId 必须在签名前放入(签名内容包含 certId), signature 最后放入(不参与签名)
    private void signParam(Map<String, Object> param) {
        param.put("signMethod", "01");
        param.put("certId", UnionSignUtil.getSignCertId(credential));
        String signature = UnionSignUtil.sign(param, credential);
        param.put("signature", signature);
    }

    /// 解析银联 form 格式响应(key=value&key=value)
    private Map<String, String> parseFormResponse(String body) {
        Map<String, String> result = new HashMap<>();
        if (StrUtil.isBlank(body)) {
            return result;
        }
        for (String kv : body.split("&")) {
            int idx = kv.indexOf('=');
            if (idx > 0) {
                String key = kv.substring(0, idx);
                String value = URLDecoder.decode(kv.substring(idx + 1), StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        return result;
    }

    /// 构建自动提交的 HTML 表单(银联前台跳转)
    private String buildAutoSubmitForm(String action, Map<String, Object> param) {
        StringBuilder sb = new StringBuilder();
        sb.append("<form action=\"").append(action).append("\" method=\"post\">");
        param.forEach((k, v) -> sb.append("<input type=\"hidden\" name=\"")
                .append(k).append("\" value=\"").append(Objects.isNull(v) ? "" : v).append("\"/>"));
        sb.append("</form>");
        sb.append("<script>document.forms[0].submit();</script>");
        return sb.toString();
    }
}
