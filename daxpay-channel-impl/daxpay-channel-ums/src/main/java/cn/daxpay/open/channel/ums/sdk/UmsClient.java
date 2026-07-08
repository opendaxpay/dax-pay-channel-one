package cn.daxpay.open.channel.ums.sdk;

import cn.daxpay.open.channel.ums.config.UmsSdkCredential;
import cn.daxpay.open.channel.ums.util.UmsDateUtil;
import cn.daxpay.open.channel.ums.util.UmsSignUtil;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/// # 银联商务 HTTP 客户端
///
/// 封装银联商务开放平台两类接口调用:
/// - **POST JSON(OPEN-BODY-SIG 签名)**: 扫码下单/查询/退款/关单, 以及 H5 的查询/退款/关单
/// - **GET URL(OPEN-FORM-PARAM 签名)**: H5 下单, 返回跳转链接(由用户浏览器发起, 非服务端调用)
///
/// 凭证([UmsSdkCredential])随请求下发, 子应用无状态, 每次调用动态构建。
/// 网关地址由 `sandbox` 标志在沙箱与生产之间切换。
///
/// HTTP 传输使用全局共享的 Spring [RestClient](由 RestClientConfiguration 提供, 底层 Apache HttpClient 5 连接池)。
@Slf4j
public class UmsClient {

    /// 沙箱环境 API 地址
    public static final String SANDBOX_API_URL = "https://test-api-open.chinaums.com";

    /// 生产环境 API 地址
    public static final String PRODUCTION_API_URL = "https://api-mop.chinaums.com";

    private final UmsSdkCredential credential;

    private final String apiUrl;

    private final RestClient restClient;

    /// 根据凭证初始化客户端, 选择沙箱/生产网关
    public UmsClient(UmsSdkCredential credential, RestClient restClient) {
        this.credential = credential;
        this.restClient = restClient;
        String rawUrl = credential.isSandbox() ? SANDBOX_API_URL : PRODUCTION_API_URL;
        this.apiUrl = StrUtil.removeSuffix(rawUrl, "/");
    }

    // ===== 扫码类接口(POST JSON, OPEN-BODY-SIG 签名) =====

    /// 扫码支付下单(返回 billQRCode 二维码链接)
    public JSONObject qrPay(Map<String, Object> param) {
        return tradePost(param, apiUrl + "/v1/netpay/bills/get-qrcode");
    }

    /// 扫码订单查询
    public JSONObject queryQrOrder(Map<String, Object> param) {
        return tradePost(param, apiUrl + "/v1/netpay/bills/query");
    }

    /// 扫码退款
    public JSONObject refundQr(Map<String, Object> param) {
        return tradePost(param, apiUrl + "/v1/netpay/bills/refund");
    }

    /// 扫码关单
    public JSONObject closeQr(Map<String, Object> param) {
        return tradePost(param, apiUrl + "/v1/netpay/bills/close-qrcode");
    }

    // ===== H5 类下单(GET URL, OPEN-FORM-PARAM 签名, 返回跳转链接) =====

    /// 支付宝 H5 支付
    public String alipayH5(Map<String, Object> param) {
        return buildH5Url(param, apiUrl + "/v1/netpay/trade/h5-pay");
    }

    /// 微信 H5 转小程序支付
    public String wechatH5ToMini(Map<String, Object> param) {
        return buildH5Url(param, apiUrl + "/v1/netpay/wxpay/h5-to-minipay");
    }

    /// 微信 H5 支付
    public String wechatH5(Map<String, Object> param) {
        return buildH5Url(param, apiUrl + "/v1/netpay/wxpay/h5-pay");
    }

    /// 银联云闪付 H5 支付
    public String unionH5(Map<String, Object> param) {
        return buildH5Url(param, apiUrl + "/v1/netpay/uac/order");
    }

    // ===== H5 类查询/退款/关单(POST JSON, OPEN-BODY-SIG 签名) =====

    /// H5 订单查询
    public JSONObject queryH5Order(Map<String, Object> param) {
        return tradePost(param, apiUrl + "/v1/netpay/query");
    }

    /// H5 退款查询
    public JSONObject queryH5Refund(Map<String, Object> param) {
        return tradePost(param, apiUrl + "/v1/netpay/refund-query");
    }

    /// H5 退款
    public JSONObject refundH5(Map<String, Object> param) {
        return tradePost(param, apiUrl + "/v1/netpay/refund");
    }

    /// H5 关单
    public JSONObject closeH5(Map<String, Object> param) {
        return tradePost(param, apiUrl + "/v1/netpay/close");
    }

    // ===== 内部方法 =====

    /// 通用 POST 请求(OPEN-BODY-SIG 签名)
    ///
    /// 银联商务所有 POST 接口共用同一签名方式, 仅 URL 不同。
    /// 响应 `errCode=SUCCESS` 表示请求被银联网关接受, 业务结果从响应体字段解析。
    private JSONObject tradePost(Map<String, Object> param, String url) {
        String body = JSONUtil.toJsonStr(param);
        String authorization = UmsSignUtil.getOpenBodySig(
                credential.getUmsAppId(), credential.getAppKey(), body);
        String resStr;
        try {
            resStr = restClient.post()
                    .uri(url)
                    .header("Authorization", authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (ChannelServiceException e) {
            throw e;
        } catch (RestClientResponseException e) {
            // 银联商务业务错误可能以非 2xx 返回, 仍需解析响应体中的 errCode
            resStr = e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("银联商务请求异常: url={}", url, e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.umsRequestFailed", e.getMessage());
        }
        log.info("银联商务请求: url={}, 请求体={}, 响应={}", url, body, resStr);
        JSONObject result = JSONUtil.parseObj(resStr);
        // errCode=SUCCESS 表示请求成功, 业务状态从其他字段判断
        if (!"SUCCESS".equals(result.getStr("errCode"))) {
            String errMsg = result.getStr("errMsg", "未知错误");
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.umsRequestFailed", errMsg);
        }
        return result;
    }

    /// 构建 H5 跳转链接(OPEN-FORM-PARAM 方式)
    ///
    /// H5 下单不是服务端直接调用银联, 而是生成带签名的 URL,
    /// 由用户浏览器跳转到银联商务收银台完成支付。
    private String buildH5Url(Map<String, Object> param, String url) {
        String timestamp = UmsDateUtil.h5Timestamp();
        String nonce = RandomUtil.randomNumbers(32);
        String reqBody = JSONUtil.toJsonStr(param);
        String signature = UmsSignUtil.getSignature(
                credential.getUmsAppId(), credential.getAppKey(), timestamp, nonce, reqBody);
        return UmsSignUtil.buildH5Url(url, credential.getUmsAppId(),
                timestamp, nonce, reqBody, signature);
    }
}
