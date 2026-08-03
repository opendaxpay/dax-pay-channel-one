package cn.daxpay.open.channel.wechat.service.direct;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.enums.WechatPayBodyType;
import cn.daxpay.open.channel.wechat.req.WechatPayReq;
import cn.daxpay.open.channel.wechat.resp.WechatPayResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.request.WxPayCodepayRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayCodepayResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.constant.WxPayErrorCode;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/// # 微信通道支付服务
///
/// 按 [WechatPayReq.method] 分发到对应支付方式子方法:
/// 扫码(NATIVE)、公众号(JSAPI)、小程序(MINI)、APP、H5、付款码(MICROPAY, V3 codepay)。
///
/// 金额单位: 微信全程使用「分」, 请求中为 Long, 调用 SDK 时转 int。
/// JSAPI/MINI/APP 由 SDK 内部完成二次签名(paySign), 返回调起参数 JSON, 透传给客户端 SDK。
/// 付款码走 V3 codepay 接口同步扣款, 用户支付中(USERPAYING)时抛业务异常由主应用轮询同步。
@Slf4j
@Service
public class WechatDirectPayService {

    /// 回调时间解析(RFC3339, 兼容带/不带小数秒、Z/+08:00 各种变体, 用于解析付款码 success_time)
    private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    /// 过期时间输出格式(微信要求 yyyy-MM-dd'T'HH:mm:ss+08:00, 无小数秒)
    private static final DateTimeFormatter EXPIRE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /// 格式化关单时间为微信要求的 RFC3339 北京时间字符串
    /// 微信要求 yyyy-MM-ddTHH:mm:ss+TIMEZONE(无小数秒), 主应用传入的 expireTime 为 UTC 偏移, 需先转 +08:00
    private static String formatExpire(OffsetDateTime expireTime) {
        return expireTime.withOffsetSameInstant(ZoneOffset.ofHours(8)).format(EXPIRE_FORMATTER);
    }

    /// 微信支付成功状态
    private static final String TRADE_STATE_SUCCESS = "SUCCESS";
    /// H5 场景类型固定值
    private static final String H5_SCENE_TYPE = "Wap";

    /// Gson(来自 weixin-java-pay 传递依赖, 用于序列化调起参数 Map → JSON)
    private static final Gson GSON = new Gson();

    /// 通道支付下单
    public WechatPayResp pay(WechatPayReq req) {
        log.info("微信通道收到支付请求: outTradeNo={}, amount={}, description={}, method={}",
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
            // 业务异常直接透传, 避免被包装成 SDK 调用异常
            throw e;
        } catch (WxPayException e) {
            log.error("微信支付调用失败: outTradeNo={}, errCode={}, errMsg={}",
                    req.getOutTradeNo(), e.getErrCode(), e.getErrCodeDes());
            // 订单已支付 / 付款码已被使用: 实际可能是之前请求已成功(结果未知), 归入结果未知由主应用查单确认
            String errCode = e.getErrCode();
            if (Objects.equals(errCode, "ORDERPAID") || Objects.equals(errCode, "AUTH_CODE_USED")) {
                throw new ChannelServiceException(ChannelErrorCode.RESULT_UNKNOWN.getCode(),
                        "channel.error.wechatPayResultUnknown", e.getMessage());
            }
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatPayCallFailed", e.getMessage());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
        return resp;
    }

    /// 扫码支付(NATIVE)
    ///
    /// 返回二维码内容(code_url), 由前端渲染成二维码供用户扫码支付。
    private void payNative(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        WxPayUnifiedOrderV3Request request = buildBaseRequest(req);
        String codeUrl = service.createOrderV3(TradeTypeEnum.NATIVE, request);
        resp.setPayBody(codeUrl);
        resp.setPayBodyType(WechatPayBodyType.QR_CODE);
    }

    /// 公众号 / 小程序支付(JSAPI)
    ///
    /// 小程序(MINI)与公众号(JSAPI)在微信侧同走 JSAPI 接口, 仅 appId 来源不同(appId 已在 config 配置)。
    /// SDK 内部完成二次签名, 返回含 appId/timeStamp/nonceStr/package/signType/paySign 的调起参数 JSON。
    private void payJsapi(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        if (StrUtil.isBlank(req.getOpenId())) {
            // 微信: JSAPI/小程序支付必填 openid
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS.getCode(),
                    "channel.error.wechatOpenIdRequired");
        }
        WxPayUnifiedOrderV3Request request = buildBaseRequest(req);
        request.setPayer(new WxPayUnifiedOrderV3Request.Payer().setOpenid(req.getOpenId()));
        WxPayUnifiedOrderV3Result.JsapiResult result = service.createOrderV3(TradeTypeEnum.JSAPI, request);
        resp.setPayBody(toJsapiPayInfoJson(result));
        resp.setPayBodyType(WechatPayBodyType.JSAPI);
    }

    /// APP 支付
    ///
    /// SDK 内部完成二次签名, 返回含 appid/partnerId/prepayId/package/noncestr/timestamp/sign 的调起参数 JSON。
    private void payApp(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        WxPayUnifiedOrderV3Request request = buildBaseRequest(req);
        WxPayUnifiedOrderV3Result.AppResult result = service.createOrderV3(TradeTypeEnum.APP, request);
        resp.setPayBody(toAppPayInfoJson(result));
        resp.setPayBodyType(WechatPayBodyType.APP_ORDER_STR);
    }

    /// H5 支付
    ///
    /// 返回支付跳转链接(h5_url), 前端可直接 location.href 跳转拉起微信客户端。
    /// 必须传场景信息(payerClientIp + h5_info)。
    private void payH5(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        if (StrUtil.isBlank(req.getPayerClientIp()) || StrUtil.isBlank(req.getWapUrl())) {
            // 微信: H5 支付必填 payerClientIp 与 wapUrl
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS.getCode(),
                    "channel.error.wechatH5SceneRequired");
        }
        WxPayUnifiedOrderV3Request request = buildBaseRequest(req);
        var sceneInfo = new WxPayUnifiedOrderV3Request.SceneInfo();
        sceneInfo.setPayerClientIp(req.getPayerClientIp());
        var h5Info = new WxPayUnifiedOrderV3Request.H5Info();
        h5Info.setType(H5_SCENE_TYPE);
        h5Info.setAppUrl(req.getWapUrl());
        h5Info.setAppName(req.getWapName());
        sceneInfo.setH5Info(h5Info);
        request.setSceneInfo(sceneInfo);
        String h5Url = service.createOrderV3(TradeTypeEnum.H5, request);
        resp.setPayBody(h5Url);
        resp.setPayBodyType(WechatPayBodyType.LINK);
    }

    /// 付款码支付(MICROPAY, V3 codepay 接口)
    ///
    /// 同步扣款: 无异常即 trade_state=SUCCESS 直接成功(complete=true);
    /// USER_PAYING(用户输入密码中) / SYSTEMERROR(系统错误) 抛业务异常, 由主应用轮询同步确认最终状态;
    /// 其他 WxPayException 由上层 catch 统一转 channel.error.wechatPayCallFailed。
    private void payCodepay(WxPayService service, WechatPayReq req, WechatPayResp resp) throws WxPayException {
        if (StrUtil.isBlank(req.getAuthCode())) {
            // 微信: 付款码支付必填 authCode
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS.getCode(),
                    "channel.error.wechatAuthCodeRequired");
        }

        var request = new WxPayCodepayRequest();
        request.setDescription(req.getDescription());
        request.setOutTradeNo(req.getOutTradeNo());
        // 金额(分)
        var amount = new WxPayCodepayRequest.Amount();
        amount.setTotal(req.getAmount().intValue());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        // 场景信息(门店信息, 微信 V3 付款码合规要求 store_info 二选一)
        var sceneInfo = new WxPayCodepayRequest.SceneInfo();
        var storeInfo = new WxPayCodepayRequest.StoreInfo();
        storeInfo.setOutId("1");
        sceneInfo.setStoreInfo(storeInfo);
        request.setSceneInfo(sceneInfo);
        // 付款码(用户出示的被扫码授权码)
        var payer = new WxPayCodepayRequest.Payer();
        payer.setAuthCode(req.getAuthCode());
        request.setPayer(payer);
        if (StrUtil.isNotBlank(req.getAttach())) {
            request.setAttach(req.getAttach());
        }

        WxPayCodepayResult result;
        try {
            result = service.codepay(request);
        } catch (WxPayException e) {
            // 用户支付中(用户输入密码) / 系统错误: 微信要求商户轮询查询确认最终状态
            // channel-one 为无状态微服务不负责轮询, 抛结果未知异常交主应用保持处理中并同步确认
            if (Objects.equals(e.getErrCode(), WxPayConstants.WxpayTradeStatus.USER_PAYING)
                    || Objects.equals(e.getResultCode(), WxPayErrorCode.UnifiedOrder.SYSTEMERROR)) {
                // 微信: 付款码用户支付中, 需主应用轮询同步
                throw new ChannelServiceException(ChannelErrorCode.RESULT_UNKNOWN.getCode(),
                        "channel.error.wechatCodepayUserPaying", e.getMessage());
            }
            throw e;
        }

        // V3 codepay: 无异常即支付成功
        resp.setTransactionId(result.getTransactionId());
        resp.setComplete(true);
        String successTime = result.getSuccessTime();
        if (StrUtil.isNotBlank(successTime)) {
            try {
                resp.setFinishTime(OffsetDateTime.parse(successTime, RFC3339_FORMATTER));
            } catch (Exception e) {
                // 时间解析失败不影响主流程
                log.warn("微信付款码完成时间解析失败: successTime={}", successTime);
            }
        }
        // 金额(分)
        WxPayCodepayResult.Amount resultAmount = result.getAmount();
        if (resultAmount != null) {
            if (resultAmount.getTotal() != null) {
                resp.setTotalAmount(resultAmount.getTotal().longValue());
            }
            if (resultAmount.getPayerTotal() != null) {
                resp.setPayerTotal(resultAmount.getPayerTotal().longValue());
            }
        }
    }

    /// 构建统一下单基础请求(V3, NATIVE/JSAPI/APP/H5 共用)
    private WxPayUnifiedOrderV3Request buildBaseRequest(WechatPayReq req) {
        var request = new WxPayUnifiedOrderV3Request();
        request.setOutTradeNo(req.getOutTradeNo());
        request.setDescription(req.getDescription());
        if (StrUtil.isNotBlank(req.getAttach())) {
            request.setAttach(req.getAttach());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            request.setNotifyUrl(req.getNotifyUrl());
        }
        // 金额(分)
        var amount = new WxPayUnifiedOrderV3Request.Amount();
        amount.setTotal(req.getAmount().intValue());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        // 过期时间(RFC3339, 无小数秒, 东八区)
        if (req.getExpireTime() != null) {
            request.setTimeExpire(formatExpire(req.getExpireTime()));
        }
        return request;
    }

    /// JSAPI 调起参数 → JSON(package 为 java 关键字, 用 packageValue 承载, 序列化为 package)
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

    /// 对象 → JSON
    private String toJson(Object obj) {
        return GSON.toJson(obj);
    }
}
