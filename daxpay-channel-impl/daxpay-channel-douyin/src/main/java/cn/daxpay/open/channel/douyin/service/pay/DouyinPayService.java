package cn.daxpay.open.channel.douyin.service.pay;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.enums.DouyinPayBodyType;
import cn.daxpay.open.channel.douyin.enums.DouyinPayMethod;
import cn.daxpay.open.channel.douyin.req.DouyinPayReq;
import cn.daxpay.open.channel.douyin.resp.DouyinPayResp;
import cn.daxpay.open.channel.douyin.utils.DouyinJsapiSigner;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.douyinpay.api.payments.app.models.ApiPrepayRequest;
import com.douyinpay.api.payments.app.models.Amount;
import com.douyinpay.api.payments.app.models.ApiSceneInfo;
import com.douyinpay.api.payments.common.ApiTransactionPayer;
import com.douyinpay.api.payments.h5.models.ApiH5Info;
import com.douyinpay.exception.DouyinpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/// # 抖音通道支付下单服务
///
/// 按 [DouyinPayReq.method] 分发到对应支付方式子方法:
/// 扫码(QR)、JSAPI、H5、APP。
/// 关单/查单不在此类, 由 close/sync Service 复用 NATIVE Service 调用。
@Slf4j
@Service
public class DouyinPayService {

    /// 货币种类
    private static final String CURRENCY_CNY = "CNY";
    /// H5 场景类型(Wap)
    private static final String H5_TYPE_WAP = "Wap";
    /// 抖音过期时间格式(RFC3339)
    private static final DateTimeFormatter RFC3339 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /// 通道支付下单
    public DouyinPayResp pay(DouyinPayReq req) {
        log.info("抖音通道收到支付请求: outTradeNo={}, amount={}, method={}",
                req.getOutTradeNo(), req.getAmount(), req.getMethod());
        DouyinPayMethod method = req.getMethod();
        if (Objects.isNull(method)) {
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS.getCode(),
                    "channel.error.douyinPayMethodNull");
        }
        return switch (method) {
            case QR -> this.nativePay(req);
            case JSAPI -> this.jsapiPay(req);
            case H5 -> this.h5Pay(req);
            case APP -> this.appPay(req);
        };
    }

    /// 扫码支付(NATIVE, 返回 codeUrl 二维码链接)
    private DouyinPayResp nativePay(DouyinPayReq req) {
        var request = new com.douyinpay.api.payments.nativepay.models.ApiPrepayRequest();
        request.setAppid(req.getCredential().getDouyinAppId());
        request.setMchid(req.getCredential().getMchId());
        request.setDescription(StrUtil.sub(req.getDescription(), 0, 127));
        request.setOutTradeNo(req.getOutTradeNo());
        request.setNotifyUrl(req.getNotifyUrl());
        // 分账订单: 透传 settle_info.profit_sharing=true
        applyAllocation(request, req);
        if (Objects.nonNull(req.getExpiredTime())) {
            request.setTimeExpire(formatRfc3339(req.getExpiredTime()));
        }
        var amount = new com.douyinpay.api.payments.nativepay.models.Amount();
        amount.setTotal(req.getAmount().intValue());
        amount.setCurrency(CURRENCY_CNY);
        request.setAmount(amount);
        if (StrUtil.isNotBlank(req.getClientIp())) {
            var sceneInfo = new com.douyinpay.api.payments.nativepay.models.ApiSceneInfo();
            sceneInfo.setPayerClientIp(req.getClientIp());
            request.setSceneInfo(sceneInfo);
        }
        try {
            var response = DouyinSdkConfig.nativeService(req.getCredential()).prepay(request);
            if (Objects.isNull(response) || StrUtil.isBlank(response.getCodeUrl())) {
                throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                        "channel.error.douyinPayFailed", "未返回二维码链接");
            }
            return new DouyinPayResp()
                    .setOutTradeNo(req.getOutTradeNo())
                    .setPayBody(response.getCodeUrl())
                    .setPayBodyType(DouyinPayBodyType.QR_CODE);
        } catch (DouyinpayException e) {
            log.error("抖音扫码支付失败", e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.douyinPayFailed", e.getMessage());
        }
    }

    /// JSAPI 支付(返回 sdk_info JSON, 需 openId)
    ///
    /// 抖音 H5 JSAPI 调起要求前端 `ttcjpay.dypay` 必须传 sdk_info JSON:
    /// `{appId, timeStamp, nonceStr, package, signType, paySign}`, 其中 paySign 由商户私钥
    /// 对 4 行签名串(appId / timeStamp / nonceStr / package)做 SHA256withRSA + Base64 得到。
    /// 与微信 WxJava SDK 内置二次签名不同, 抖音 SDK 只返回 prepayId, 必须在此自行组装。
    private DouyinPayResp jsapiPay(DouyinPayReq req) {
        if (StrUtil.isBlank(req.getOpenId())) {
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS.getCode(),
                    "channel.error.douyinJsapiNoOpenId");
        }
        var request = new com.douyinpay.api.payments.jsapi.models.ApiPrepayRequest();
        request.setAppid(req.getCredential().getDouyinAppId());
        request.setMchid(req.getCredential().getMchId());
        request.setDescription(StrUtil.sub(req.getDescription(), 0, 127));
        request.setOutTradeNo(req.getOutTradeNo());
        request.setNotifyUrl(req.getNotifyUrl());
        // 分账订单: 透传 settle_info.profit_sharing=true
        applyAllocation(request, req);
        if (Objects.nonNull(req.getExpiredTime())) {
            request.setTimeExpire(formatRfc3339(req.getExpiredTime()));
        }
        var amount = new com.douyinpay.api.payments.jsapi.models.Amount();
        amount.setTotal(req.getAmount().intValue());
        amount.setCurrency(CURRENCY_CNY);
        request.setAmount(amount);
        if (StrUtil.isNotBlank(req.getClientIp())) {
            var sceneInfo = new com.douyinpay.api.payments.jsapi.models.ApiSceneInfo();
            sceneInfo.setPayerClientIp(req.getClientIp());
            request.setSceneInfo(sceneInfo);
        }
        var payer = new ApiTransactionPayer();
        payer.setOpenid(req.getOpenId());
        request.setPayerInfo(payer);
        try {
            var response = DouyinSdkConfig.jsapiService(req.getCredential()).prepay(request);
            if (Objects.isNull(response) || StrUtil.isBlank(response.getPrepayId())) {
                throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                        "channel.error.douyinPayFailed", "未返回prepay_id");
            }
            // 组装前端 ttcjpay.dypay 所需 sdk_info JSON
            String appId = req.getCredential().getDouyinAppId();
            String timeStamp = String.valueOf(Instant.now().getEpochSecond());
            String nonceStr = RandomUtil.randomString(32);
            String packageValue = "prepay_id=" + response.getPrepayId();
            String paySign = DouyinJsapiSigner.signPayInfo(
                    appId, timeStamp, nonceStr, response.getPrepayId(),
                    req.getCredential().getMerchantPrivateKey());
            Map<String, String> sdkInfo = new LinkedHashMap<>();
            sdkInfo.put("appId", appId);
            sdkInfo.put("timeStamp", timeStamp);
            sdkInfo.put("nonceStr", nonceStr);
            sdkInfo.put("package", packageValue);
            sdkInfo.put("signType", "DouyinPay-RSA");
            sdkInfo.put("paySign", paySign);
            return new DouyinPayResp()
                    .setOutTradeNo(req.getOutTradeNo())
                    .setPayBody(JSONUtil.toJsonStr(sdkInfo))
                    .setPayBodyType(DouyinPayBodyType.JSAPI);
        } catch (DouyinpayException e) {
            log.error("抖音JSAPI支付失败", e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.douyinPayFailed", e.getMessage());
        }
    }

    /// APP 支付(返回 prepayId)
    private DouyinPayResp appPay(DouyinPayReq req) {
        var request = new ApiPrepayRequest();
        request.setAppid(req.getCredential().getDouyinAppId());
        request.setMchid(req.getCredential().getMchId());
        request.setDescription(StrUtil.sub(req.getDescription(), 0, 127));
        request.setOutTradeNo(req.getOutTradeNo());
        request.setNotifyUrl(req.getNotifyUrl());
        // 分账订单: 透传 settle_info.profit_sharing=true
        applyAllocation(request, req);
        if (Objects.nonNull(req.getExpiredTime())) {
            request.setTimeExpire(formatRfc3339(req.getExpiredTime()));
        }
        var amount = new Amount();
        amount.setTotal(req.getAmount().intValue());
        amount.setCurrency(CURRENCY_CNY);
        request.setAmount(amount);
        if (StrUtil.isNotBlank(req.getClientIp())) {
            var sceneInfo = new ApiSceneInfo();
            sceneInfo.setPayerClientIp(req.getClientIp());
            request.setSceneInfo(sceneInfo);
        }
        try {
            var response = DouyinSdkConfig.appService(req.getCredential()).prepay(request);
            if (Objects.isNull(response) || StrUtil.isBlank(response.getPrepayId())) {
                throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                        "channel.error.douyinPayFailed", "未返回prepay_id");
            }
            return new DouyinPayResp()
                    .setOutTradeNo(req.getOutTradeNo())
                    .setPayBody(response.getPrepayId())
                    .setPayBodyType(DouyinPayBodyType.IDENTIFIER);
        } catch (DouyinpayException e) {
            log.error("抖音APP支付失败", e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.douyinPayFailed", e.getMessage());
        }
    }

    /// H5 支付(返回 h5Url 跳转链接)
    private DouyinPayResp h5Pay(DouyinPayReq req) {
        var request = new com.douyinpay.api.payments.h5.models.ApiPrepayRequest();
        request.setAppid(req.getCredential().getDouyinAppId());
        request.setMchid(req.getCredential().getMchId());
        request.setDescription(StrUtil.sub(req.getDescription(), 0, 127));
        request.setOutTradeNo(req.getOutTradeNo());
        request.setNotifyUrl(req.getNotifyUrl());
        // 分账订单: 透传 settle_info.profit_sharing=true
        applyAllocation(request, req);
        if (Objects.nonNull(req.getExpiredTime())) {
            request.setTimeExpire(formatRfc3339(req.getExpiredTime()));
        }
        var amount = new com.douyinpay.api.payments.h5.models.Amount();
        amount.setTotal(req.getAmount().intValue());
        amount.setCurrency(CURRENCY_CNY);
        request.setAmount(amount);
        var sceneInfo = new com.douyinpay.api.payments.h5.models.ApiSceneInfo();
        if (StrUtil.isNotBlank(req.getClientIp())) {
            sceneInfo.setPayerClientIp(req.getClientIp());
        }
        var h5Info = new ApiH5Info();
        h5Info.setType(H5_TYPE_WAP);
        sceneInfo.setH5Info(h5Info);
        request.setSceneInfo(sceneInfo);
        try {
            var response = DouyinSdkConfig.h5Service(req.getCredential()).prepay(request);
            if (Objects.isNull(response) || StrUtil.isBlank(response.getH5Url())) {
                throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                        "channel.error.douyinPayFailed", "未返回h5_url");
            }
            return new DouyinPayResp()
                    .setOutTradeNo(req.getOutTradeNo())
                    .setPayBody(response.getH5Url())
                    .setPayBodyType(DouyinPayBodyType.LINK);
        } catch (DouyinpayException e) {
            log.error("抖音H5支付失败", e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.douyinPayFailed", e.getMessage());
        }
    }

    /// OffsetDateTime 转 RFC3339 字符串(抖音要求 ISO_OFFSET_DATETIME 格式)
    private String formatRfc3339(OffsetDateTime time) {
        return time.withOffsetSameInstant(ZoneOffset.ofHours(8)).format(RFC3339);
    }

    /// 分账订单标识透传: 反射构建 SettleInfo 并设置 profit_sharing=true
    ///
    /// 抖音各支付方式 ApiPrepayRequest 分属不同包(nativepay/app/h5/jsapi),
    /// ApiSettleInfo 也各自独立, 此处反射统一处理。
    private static void applyAllocation(Object request, DouyinPayReq req) {
        if (!Boolean.TRUE.equals(req.getAllocation())) {
            return;
        }
        try {
            // 按请求类型找到同包的 ApiSettleInfo
            String settleClassName = request.getClass().getPackageName() + ".ApiSettleInfo";
            Class<?> settleClass = Class.forName(settleClassName);
            var settleInfo = settleClass.getDeclaredConstructor().newInstance();
            var setProfitSharing = settleClass.getMethod("setProfitSharing", Boolean.class);
            setProfitSharing.invoke(settleInfo, Boolean.TRUE);
            var setSettleInfo = request.getClass().getMethod("setSettleInfo", settleClass);
            setSettleInfo.invoke(request, settleInfo);
        } catch (Exception e) {
            // SettleInfo 类不存在(不应发生), 静默跳过
        }
    }
}
