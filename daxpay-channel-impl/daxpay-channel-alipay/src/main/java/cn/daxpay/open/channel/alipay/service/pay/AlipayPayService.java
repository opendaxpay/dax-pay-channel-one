package cn.daxpay.open.channel.alipay.service.pay;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.AlipayResponse;
import com.alipay.api.domain.*;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.alipay.enums.AlipayPayBodyType;
import cn.daxpay.open.channel.alipay.req.AlipayPayReq;
import cn.daxpay.open.channel.alipay.resp.AlipayPayResp;
import cn.daxpay.open.platform.common.util.PayUtil;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/// # 支付宝通道支付服务
///
/// 按 [AlipayPayReq.method] 分发到对应支付方式子方法:
/// 手机网站(WAP)、APP、电脑网站(PC)、扫码预下单(QR)、付款码(BARCODE)、小程序/JSAPI(JSAPI)。
@Slf4j
@Service
public class AlipayPayService {

    /// 支付宝网关成功响应码
    private static final String CODE_SUCCESS = "10000";
    /// 支付宝付款码"支付处理中"响应码(需主应用轮询确认最终状态)
    private static final String CODE_IN_PROCESS = "10003";
    /// 付款码支付场景
    private static final String SCENE_BAR_CODE = "bar_code";
    /// 小程序/JSAPI 支付产品码
    private static final String PRODUCT_JSAPI = "JSAPI_PAY";
    /// 支付宝买家ID前缀(2088 开头为支付宝用户ID)
    private static final String BUYER_ID_PREFIX = "2088";

    /// 支付宝过期时间格式(yyyy-MM-dd HH:mm:ss, 支付宝服务器时区 GMT+8)
    private static final DateTimeFormatter EXPIRE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /// 格式化关单时间为支付宝要求的北京时间字符串
    /// 主应用传入的 expireTime 为 UTC 偏移, 支付宝 time_expire 按服务器时区(GMT+8)解析, 需先转 +08:00 再格式化
    private static String formatExpire(OffsetDateTime expireTime) {
        return expireTime.withOffsetSameInstant(ZoneOffset.ofHours(8)).format(EXPIRE_FORMATTER);
    }

    /// 通道支付下单
    ///
    /// 金额单位转换: 请求中为分, 调用 SDK 时转为元(保留两位小数)。
    public AlipayPayResp pay(AlipayPayReq req) {
        log.info("支付宝通道收到支付请求: outTradeNo={}, amount={}, subject={}, method={}",
                req.getOutTradeNo(), req.getAmount(), req.getSubject(), req.getMethod());

        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        // 金额单位转换: 请求中为分(整型), 调用支付宝 SDK 需转为元(保留两位小数)
        String amount = PayUtil.conversionFenToYuan(req.getAmount()).toPlainString();
        AlipayPayResp resp = new AlipayPayResp();
        resp.setOutTradeNo(req.getOutTradeNo());
        // 默认未终态完成, 仅 BARCODE 付款码同步成功时覆盖为 true
        resp.setComplete(false);
        try {
            switch (req.getMethod()) {
                case WAP -> payWap(client, req, amount, resp);
                case APP -> payApp(client, req, amount, resp);
                case PC -> payPc(client, req, amount, resp);
                case QR -> payQr(client, req, amount, resp);
                case BARCODE -> payBarcode(client, req, amount, resp);
                case JSAPI -> payJsapi(client, req, amount, resp);
            }
        } catch (ChannelServiceException e) {
            // 业务异常直接透传, 避免被包装成 SDK 调用异常
            throw e;
        } catch (AlipayApiException e) {
            // SDK 调用异常(网络/签名等), 保留支付宝原始错误信息, 避免丢码
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayPayCallFailed", e.getErrMsg());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
        return resp;
    }

    /// 手机网站支付(WAP, QUICK_WAP_WAY)
    ///
    /// 通过 GET 方式返回可直接跳转的支付链接, 前端 `location.href` 即可, 无需整页渲染表单。
    private void payWap(AlipayClient client, AlipayPayReq req, String amount, AlipayPayResp resp) throws AlipayApiException {
        var request = new AlipayTradeWapPayRequest();
        var model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(req.getOutTradeNo());
        model.setTotalAmount(amount);
        model.setSubject(req.getSubject());
        model.setBody(req.getBody());
        model.setProductCode("QUICK_WAP_WAY");
        request.setBizModel(model);
        // 服务商模式: 注入应用授权令牌
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            request.setNotifyUrl(req.getNotifyUrl());
        }
        if (req.getExpireTime() != null) {
            model.setTimeExpire(formatExpire(req.getExpireTime()));
        }
        // GET 方式返回可直接跳转的 URL
        AlipayTradeWapPayResponse alipayResp = client.pageExecute(request, "GET");
        verifySuccess(alipayResp);
        resp.setPayBody(alipayResp.getBody());
        resp.setPayBodyType(AlipayPayBodyType.LINK);
    }

    /// APP 支付(QUICK_MSECURITY_PAY)
    ///
    /// 返回订单串(orderStr), 由客户端 SDK 唤起支付宝 APP 完成支付。
    private void payApp(AlipayClient client, AlipayPayReq req, String amount, AlipayPayResp resp) throws AlipayApiException {
        var request = new AlipayTradeAppPayRequest();
        var model = new AlipayTradeAppPayModel();
        model.setOutTradeNo(req.getOutTradeNo());
        model.setTotalAmount(amount);
        model.setSubject(req.getSubject());
        model.setBody(req.getBody());
        model.setProductCode("QUICK_MSECURITY_PAY");
        request.setBizModel(model);
        // 服务商模式: 注入应用授权令牌
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            request.setNotifyUrl(req.getNotifyUrl());
        }
        if (req.getExpireTime() != null) {
            model.setTimeExpire(formatExpire(req.getExpireTime()));
        }
        AlipayTradeAppPayResponse alipayResp = client.sdkExecute(request);
        verifySuccess(alipayResp);
        resp.setPayBody(alipayResp.getBody());
        resp.setPayBodyType(AlipayPayBodyType.ORDER_STR);
    }

    /// 电脑网站支付(PC, FAST_INSTANT_TRADE_PAY)
    ///
    /// 通过 GET 方式返回可直接跳转的支付链接, 前端 `location.href` 即可, 无需整页渲染表单。
    private void payPc(AlipayClient client, AlipayPayReq req, String amount, AlipayPayResp resp) throws AlipayApiException {
        var request = new AlipayTradePagePayRequest();
        var model = new AlipayTradePagePayModel();
        model.setOutTradeNo(req.getOutTradeNo());
        model.setTotalAmount(amount);
        model.setSubject(req.getSubject());
        model.setBody(req.getBody());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        request.setBizModel(model);
        // 服务商模式: 注入应用授权令牌
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            request.setNotifyUrl(req.getNotifyUrl());
        }
        if (req.getExpireTime() != null) {
            model.setTimeExpire(formatExpire(req.getExpireTime()));
        }
        // GET 方式返回可直接跳转的 URL
        AlipayTradePagePayResponse alipayResp = client.pageExecute(request, "GET");
        verifySuccess(alipayResp);
        resp.setPayBody(alipayResp.getBody());
        resp.setPayBodyType(AlipayPayBodyType.LINK);
    }

    /// 扫码预下单(QR, precreate)
    ///
    /// 返回二维码内容(qrCode), 由前端渲染成二维码供用户扫码支付。
    private void payQr(AlipayClient client, AlipayPayReq req, String amount, AlipayPayResp resp) throws AlipayApiException {
        var request = new AlipayTradePrecreateRequest();
        var model = new AlipayTradePrecreateModel();
        model.setOutTradeNo(req.getOutTradeNo());
        model.setTotalAmount(amount);
        model.setSubject(req.getSubject());
        model.setBody(req.getBody());
        request.setBizModel(model);
        // 服务商模式: 注入应用授权令牌
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            request.setNotifyUrl(req.getNotifyUrl());
        }
        if (req.getExpireTime() != null) {
            model.setTimeExpire(formatExpire(req.getExpireTime()));
        }
        AlipayTradePrecreateResponse alipayResp = AlipaySdkConfig.execute(client, req.getCredential(), request);
        verifySuccess(alipayResp);
        resp.setPayBody(alipayResp.getQrCode());
        resp.setPayBodyType(AlipayPayBodyType.QR_CODE);
    }

    /// 付款码支付(BARCODE, 当面付 bar_code 场景)
    ///
    /// 同步扣款: `code=10000` 直接成功(complete=true); `code=10003` 支付处理中(需主应用轮询); 其他抛业务异常。
    private void payBarcode(AlipayClient client, AlipayPayReq req, String amount, AlipayPayResp resp) throws AlipayApiException {
        var request = new AlipayTradePayRequest();
        var model = new AlipayTradePayModel();
        model.setOutTradeNo(req.getOutTradeNo());
        model.setTotalAmount(amount);
        model.setSubject(req.getSubject());
        model.setBody(req.getBody());
        model.setScene(SCENE_BAR_CODE);
        model.setAuthCode(req.getAuthCode());
        request.setBizModel(model);
        // 服务商模式: 注入应用授权令牌
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            request.setNotifyUrl(req.getNotifyUrl());
        }
        if (req.getExpireTime() != null) {
            model.setTimeExpire(formatExpire(req.getExpireTime()));
        }
        AlipayTradePayResponse alipayResp = AlipaySdkConfig.execute(client, req.getCredential(), request);
        String code = alipayResp.getCode();
        // 支付成功, 记录完成信息
        if (CODE_SUCCESS.equals(code)) {
            resp.setTradeNo(alipayResp.getTradeNo());
            resp.setComplete(true);
            Date gmtPayment = alipayResp.getGmtPayment();
            if (gmtPayment != null) {
                resp.setFinishTime(OffsetDateTime.ofInstant(gmtPayment.toInstant(), ZoneId.systemDefault()));
            }
            // 金额(元转分)
            resp.setTotalAmount(toFen(alipayResp.getTotalAmount()));
            resp.setBuyerPayAmount(toFen(alipayResp.getBuyerPayAmount()));
            resp.setReceiptAmount(toFen(alipayResp.getReceiptAmount()));
            // 用户标识
            resp.setBuyerUserId(alipayResp.getBuyerUserId());
            resp.setBuyerOpenId(alipayResp.getBuyerOpenId());
            resp.setBuyerLogonId(alipayResp.getBuyerLogonId());
        }
        // 非支付处理中(10003)的响应码, 进行错误校验(成功码 10000 时 isSuccess 为 true 不会抛异常)
        if (!CODE_IN_PROCESS.equals(code)) {
            verifySuccess(alipayResp);
        }
    }

    /// 小程序/JSAPI 支付(JSAPI_PAY, `alipay.trade.create` 接口)
    ///
    /// 返回支付宝交易号(tradeNo), 由小程序 SDK 调起支付。
    /// 买家标识 openId: `2088` 开头为支付宝用户ID(buyer_id), 否则视为小程序 openid。
    private void payJsapi(AlipayClient client, AlipayPayReq req, String amount, AlipayPayResp resp) throws AlipayApiException {
        var request = new AlipayTradeCreateRequest();
        var model = new AlipayTradeCreateModel();
        model.setOutTradeNo(req.getOutTradeNo());
        model.setTotalAmount(amount);
        model.setSubject(req.getSubject());
        model.setBody(req.getBody());
        model.setProductCode(PRODUCT_JSAPI);
        // 买家标识: 2088 开头是支付宝用户ID, 否则为小程序 openid
        String openId = req.getOpenId();
        if (StrUtil.startWith(openId, BUYER_ID_PREFIX)) {
            model.setBuyerId(openId);
        } else {
            model.setOpBuyerOpenId(openId);
        }
        request.setBizModel(model);
        // 服务商模式: 注入应用授权令牌
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            request.setNotifyUrl(req.getNotifyUrl());
        }
        if (req.getExpireTime() != null) {
            model.setTimeExpire(formatExpire(req.getExpireTime()));
        }
        AlipayTradeCreateResponse alipayResp = AlipaySdkConfig.execute(client, req.getCredential(), request);
        verifySuccess(alipayResp);
        resp.setTradeNo(alipayResp.getTradeNo());
        resp.setPayBody(alipayResp.getTradeNo());
        resp.setPayBodyType(AlipayPayBodyType.IDENTIFIER);
    }

    /// 元字符串转分(Long), 空白返回 null
    private static Long toFen(String yuan) {
        if (StrUtil.isBlank(yuan)) {
            return null;
        }
        return (long) PayUtil.conversionYuanToFenHalfUp(yuan);
    }

    /// 校验支付宝响应是否成功, 失败则抛业务异常(保留 subCode/subMsg 错误信息)
    private void verifySuccess(AlipayResponse alipayResponse) {
        if (!alipayResponse.isSuccess()) {
            String errorMsg = StrUtil.blankToDefault(alipayResponse.getSubMsg(), alipayResponse.getMsg());
            log.error("支付宝支付失败: code={}, subCode={}, subMsg={}",
                    alipayResponse.getCode(), alipayResponse.getSubCode(), alipayResponse.getSubMsg());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayPayCallFailed", errorMsg);
        }
    }
}
