package cn.daxpay.open.channel.wechat.service.isv;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.enums.WechatPayBodyType;
import cn.daxpay.open.channel.wechat.req.WechatPayReq;
import cn.daxpay.open.channel.wechat.resp.WechatPayResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.request.WxPayPartnerUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.constant.WxPayErrorCode;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/// # 微信服务商通道支付服务
///
/// 按 [WechatPayReq.method] 分发到对应支付方式子方法(服务商模式):
/// 扫码(NATIVE)、公众号/小程序(JSAPI/MINI)、APP、H5、付款码(MICROPAY)。
///
/// 与直连模式([cn.daxpay.open.channel.wechat.service.direct.WechatDirectPayService])的核心差异:
/// - 下单调用 `createPartnerOrderV3` + [WxPayPartnerUnifiedOrderV3Request], 走 `/v3/partner/transactions/*` 路径
/// - 请求体由 sp_appid/sp_mchid/sub_mchid 标识服务商与特约商户身份
/// - JSAPI/MINI 的 payer 用 sub_openid(配置了 sub_appid 时) 或 sp_openid
/// - 付款码 WxJava 未封装服务商接口, 手动 `postV3 /v3/pay/partner/transactions/codepay`
///
/// 金额单位: 微信全程使用「分」, 请求中为 Long, 调用 SDK 时转 int。
@Slf4j
@Service
public class WechatIsvPayService {

    /// 回调时间解析(RFC3339, 兼容带/不带小数秒、Z/+08:00 各种变体)
    private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    /// 过期时间输出格式(微信要求 yyyy-MM-dd'T'HH:mm:ss+08:00, 无小数秒)
    private static final DateTimeFormatter EXPIRE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /// 格式化关单时间为微信要求的 RFC3339 北京时间字符串
    /// 微信要求 yyyy-MM-ddTHH:mm:ss+TIMEZONE(无小数秒), 主应用传入的 expireTime 为 UTC 偏移, 需先转 +08:00
    private static String formatExpire(OffsetDateTime expireTime) {
        return expireTime.withOffsetSameInstant(ZoneOffset.ofHours(8)).format(EXPIRE_FORMATTER);
    }

    /// H5 场景类型固定值
    private static final String H5_SCENE_TYPE = "Wap";
    /// Gson(weixin-java-pay 传递依赖, 序列化调起参数 Map → JSON 与付款码请求体)
    private static final Gson GSON = new GsonBuilder().create();

    /// 服务商通道支付下单
    public WechatPayResp pay(WechatPayReq req) {
        log.info("微信服务商通道收到支付请求: outTradeNo={}, amount={}, description={}, method={}",
                req.getOutTradeNo(), req.getAmount(), req.getDescription(), req.getMethod());

        WxPayService service = WechatSdkConfig.buildService(req.getCredential());
        WechatPayResp resp = new WechatPayResp();
        resp.setOutTradeNo(req.getOutTradeNo());
        // 默认未终态完成, 仅 MICROPAY 付款码同步成功时覆盖为 true
        resp.setComplete(false);
        try {
            switch (req.getMethod()) {
                case NATIVE -> payNative(service, req, resp);
                case JSAPI, MINI -> payJsapi(service, req, resp);
                case APP -> payApp(service, req, resp);
                case H5 -> payH5(service, req, resp);
                case MICROPAY -> payCodepay(service, req, resp);
            }
        } catch (ChannelServiceException e) {
            // 业务异常直接透传
            throw e;
        } catch (WxPayException e) {
            log.error("微信服务商支付调用失败: outTradeNo={}, errCode={}, errMsg={}",
                    req.getOutTradeNo(), e.getErrCode(), e.getErrCodeDes());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatPayCallFailed", e.getMessage());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
        return resp;
    }

    /// 扫码支付(NATIVE, 服务商)
    private void payNative(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        WxPayPartnerUnifiedOrderV3Request request = buildBaseRequest(req);
        String codeUrl = service.createPartnerOrderV3(TradeTypeEnum.NATIVE, request);
        resp.setPayBody(codeUrl);
        resp.setPayBodyType(WechatPayBodyType.QR_CODE);
    }

    /// 公众号 / 小程序支付(JSAPI/MINI, 服务商)
    ///
    /// sub_appid 配置时 payer 用 sub_openid, 否则用 sp_openid(对标商业版 WechatPaySubService.jsPay)
    private void payJsapi(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        if (StrUtil.isBlank(req.getOpenId())) {
            // 微信: JSAPI/小程序支付必填 openid
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS.getCode(),
                    "channel.error.wechatOpenIdRequired");
        }
        WxPayPartnerUnifiedOrderV3Request request = buildBaseRequest(req);
        WxPayPartnerUnifiedOrderV3Request.Payer payer = new WxPayPartnerUnifiedOrderV3Request.Payer();
        var credential = req.getCredential();
        if (StrUtil.isNotBlank(credential.getSubAppId())) {
            // 配置了子应用 → sub_openid
            request.setSubAppid(credential.getSubAppId());
            payer.setSubOpenid(req.getOpenId());
        } else {
            // 未配置子应用 → sp_openid
            payer.setSpOpenid(req.getOpenId());
        }
        request.setPayer(payer);
        WxPayUnifiedOrderV3Result.JsapiResult result = service.createPartnerOrderV3(TradeTypeEnum.JSAPI, request);
        resp.setPayBody(toJsapiPayInfoJson(result));
        resp.setPayBodyType(WechatPayBodyType.JSAPI);
    }

    /// APP 支付(服务商)
    private void payApp(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        WxPayPartnerUnifiedOrderV3Request request = buildBaseRequest(req);
        WxPayUnifiedOrderV3Result.AppResult result = service.createPartnerOrderV3(TradeTypeEnum.APP, request);
        resp.setPayBody(toAppPayInfoJson(result));
        resp.setPayBodyType(WechatPayBodyType.APP_ORDER_STR);
    }

    /// H5 支付(服务商)
    private void payH5(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        if (StrUtil.isBlank(req.getPayerClientIp()) || StrUtil.isBlank(req.getWapUrl())) {
            // 微信: H5 支付必填 payerClientIp 与 wapUrl
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS.getCode(),
                    "channel.error.wechatH5SceneRequired");
        }
        WxPayPartnerUnifiedOrderV3Request request = buildBaseRequest(req);
        var sceneInfo = new WxPayPartnerUnifiedOrderV3Request.SceneInfo();
        sceneInfo.setPayerClientIp(req.getPayerClientIp());
        var h5Info = new WxPayPartnerUnifiedOrderV3Request.H5Info();
        h5Info.setType(H5_SCENE_TYPE);
        h5Info.setAppUrl(req.getWapUrl());
        h5Info.setAppName(req.getWapName());
        sceneInfo.setH5Info(h5Info);
        request.setSceneInfo(sceneInfo);
        String h5Url = service.createPartnerOrderV3(TradeTypeEnum.H5, request);
        resp.setPayBody(h5Url);
        resp.setPayBodyType(WechatPayBodyType.LINK);
    }

    /// 付款码支付(MICROPAY, 服务商)
    ///
    /// WxJava 4.8.x 未封装服务商付款码接口, 手动 `postV3 /v3/pay/partner/transactions/codepay`。
    /// 用户支付中(USERPAYING) / 系统错误(SYSTEMERROR) 抛业务异常由主应用轮询同步。
    private void payCodepay(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        if (StrUtil.isBlank(req.getAuthCode())) {
            // 微信: 付款码支付必填 authCode
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS.getCode(),
                    "channel.error.wechatAuthCodeRequired");
        }
        var credential = req.getCredential();
        var config = service.getConfig();

        // 构建 partner codepay 请求体(蛇形字段名, 对齐微信 V3 文档)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sp_appid", config.getAppId());
        body.put("sp_mchid", config.getMchId());
        body.put("sub_mchid", credential.getSubMchId());
        body.put("description", req.getDescription());
        body.put("out_trade_no", req.getOutTradeNo());
        // 金额(分)
        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("total", req.getAmount().intValue());
        amount.put("currency", "CNY");
        body.put("amount", amount);
        // 场景信息(门店信息, 微信 V3 付款码合规要求 store_info 二选一)
        Map<String, Object> sceneInfo = new LinkedHashMap<>();
        Map<String, Object> storeInfo = new LinkedHashMap<>();
        storeInfo.put("out_id", "1");
        sceneInfo.put("store_info", storeInfo);
        body.put("scene_info", sceneInfo);
        // 付款码
        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("auth_code", req.getAuthCode());
        body.put("payer", payer);
        if (StrUtil.isNotBlank(req.getAttach())) {
            body.put("attach", req.getAttach());
        }

        String url = String.format("%s/v3/pay/partner/transactions/codepay", service.getPayBaseUrl());
        String responseBody;
        try {
            responseBody = service.postV3(url, GSON.toJson(body));
        } catch (WxPayException e) {
            // 用户支付中 / 系统错误: 微信要求商户轮询查询确认最终状态
            if (Objects.equals(e.getErrCode(), WxPayConstants.WxpayTradeStatus.USER_PAYING)
                    || Objects.equals(e.getResultCode(), WxPayErrorCode.UnifiedOrder.SYSTEMERROR)) {
                throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                        "channel.error.wechatCodepayUserPaying", e.getMessage());
            }
            throw e;
        }

        // 解析响应(无异常即支付成功)
        CodepayResult result = GSON.fromJson(responseBody, CodepayResult.class);
        resp.setTransactionId(result.transactionId);
        resp.setComplete(true);
        if (StrUtil.isNotBlank(result.successTime)) {
            try {
                resp.setFinishTime(OffsetDateTime.parse(result.successTime, RFC3339_FORMATTER));
            } catch (Exception e) {
                log.warn("微信服务商付款码完成时间解析失败: successTime={}", result.successTime);
            }
        }
        // 金额(分)
        if (result.amount != null) {
            if (result.amount.total != null) {
                resp.setTotalAmount(result.amount.total.longValue());
            }
            if (result.amount.payerTotal != null) {
                resp.setPayerTotal(result.amount.payerTotal.longValue());
            }
        }
    }

    /// 构建服务商统一下单基础请求(sp_appid/sp_mchid/sub_mchid 由 SDK 从 config 自动填充, sub_appid 在调用处按需设置)
    private WxPayPartnerUnifiedOrderV3Request buildBaseRequest(WechatPayReq req) {
        var request = new WxPayPartnerUnifiedOrderV3Request();
        request.setOutTradeNo(req.getOutTradeNo());
        request.setDescription(req.getDescription());
        if (StrUtil.isNotBlank(req.getAttach())) {
            request.setAttach(req.getAttach());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            request.setNotifyUrl(req.getNotifyUrl());
        }
        // 金额(分)
        var amount = new WxPayPartnerUnifiedOrderV3Request.Amount();
        amount.setTotal(req.getAmount().intValue());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        // 过期时间(RFC3339, 无小数秒, 东八区)
        if (req.getExpireTime() != null) {
            request.setTimeExpire(formatExpire(req.getExpireTime()));
        }
        return request;
    }

    /// JSAPI 调起参数 → JSON
    private String toJsapiPayInfoJson(WxPayUnifiedOrderV3Result.JsapiResult r) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("appId", r.getAppId());
        map.put("timeStamp", r.getTimeStamp());
        map.put("nonceStr", r.getNonceStr());
        map.put("package", r.getPackageValue());
        map.put("signType", r.getSignType());
        map.put("paySign", r.getPaySign());
        return toJson(map);
    }

    /// APP 调起参数 → JSON
    private String toAppPayInfoJson(WxPayUnifiedOrderV3Result.AppResult r) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("appid", r.getAppid());
        map.put("partnerid", r.getPartnerId());
        map.put("prepayid", r.getPrepayId());
        map.put("package", r.getPackageValue());
        map.put("noncestr", r.getNoncestr());
        map.put("timestamp", r.getTimestamp());
        map.put("sign", r.getSign());
        return toJson(map);
    }

    private String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /// 服务商付款码响应解析(微信 V3 codepay 返回字段蛇形命名)
    private static class CodepayResult {
        @SerializedName("transaction_id")
        String transactionId;
        @SerializedName("success_time")
        String successTime;
        Amount amount;

        private static class Amount {
            @SerializedName("total")
            Integer total;
            @SerializedName("payer_total")
            Integer payerTotal;
        }
    }
}
