package cn.daxpay.open.channel.alipay.service;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.*;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.platform.core.dto.pay.ChannelPayReq;
import cn.daxpay.open.platform.core.dto.pay.ChannelPayResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.daxpay.open.platform.core.service.ChannelPayService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/// # 支付宝通道支付服务
///
/// 实现 [ChannelPayService], 按 `method` 支持以下支付方式:
/// `alipay_wap`(手机网站)、`alipay_app`(APP)、`alipay_page`(电脑网站)、`alipay_qr`(扫码预下单)。
/// 当请求 `config` 为空时进入 Demo 模式, 返回模拟支付响应, 不调用真实支付宝 SDK。
@Slf4j
@Service
public class AlipayPayService implements ChannelPayService {

    /// 通道支付下单
    ///
    /// 金额单位转换: 请求中为分, 调用 SDK 时转为元(保留两位小数)。
    @Override
    public ChannelPayResp pay(ChannelPayReq req) {
        log.info("📋 支付宝通道收到支付请求: bizOrderNo={}, amount={}, subject={}, method={}",
                req.getBizOrderNo(), req.getAmount(), req.getSubject(), req.getMethod());

        // Demo 模式: 无通道配置时返回模拟响应, 不调用真实支付宝 SDK
        if (req.getConfig() == null || req.getConfig().isEmpty()) {
            log.info("🧪 Demo 模式: 无通道配置, 返回模拟支付响应");
            ChannelPayResp demoResp = new ChannelPayResp();
            demoResp.setBizOrderNo(req.getBizOrderNo());
            demoResp.setOutOrderNo("DEMO_" + System.currentTimeMillis());
            demoResp.setComplete(false);
            demoResp.setPayBody("https://open.alipay.com/demo/pay?orderNo=" + demoResp.getOutOrderNo());
            demoResp.setPayBodyType("qr_code");
            return demoResp;
        }

        AlipayClient client = AlipaySdkConfig.buildClient(req.getConfig());
        String amount = new BigDecimal(req.getAmount()).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP).toPlainString();
        ChannelPayResp resp = new ChannelPayResp();
        resp.setBizOrderNo(req.getBizOrderNo());
        String method = req.getMethod();
        try {
            if ("alipay_wap".equals(method)) {
                AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
                AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
                model.setOutTradeNo(req.getBizOrderNo());
                model.setTotalAmount(amount);
                model.setSubject(req.getSubject());
                model.setBody(req.getDescription());
                model.setProductCode("QUICK_WAP_WAY");
                request.setBizModel(model);
                if (StrUtil.isNotBlank(req.getExpireTime())) model.setTimeExpire(req.getExpireTime());
                AlipayTradeWapPayResponse alipayResp = client.pageExecute(request);
                resp.setPayBody(alipayResp.getBody());
                resp.setPayBodyType("form");
                if (StrUtil.isNotBlank(alipayResp.getOutTradeNo())) resp.setOutOrderNo(alipayResp.getOutTradeNo());
            } else if ("alipay_app".equals(method)) {
                AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
                AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
                model.setOutTradeNo(req.getBizOrderNo());
                model.setTotalAmount(amount);
                model.setSubject(req.getSubject());
                model.setBody(req.getDescription());
                model.setProductCode("QUICK_MSECURITY_PAY");
                request.setBizModel(model);
                if (StrUtil.isNotBlank(req.getExpireTime())) model.setTimeExpire(req.getExpireTime());
                AlipayTradeAppPayResponse alipayResp = client.sdkExecute(request);
                resp.setPayBody(alipayResp.getBody());
                resp.setPayBodyType("order_id");
                if (StrUtil.isNotBlank(alipayResp.getOutTradeNo())) resp.setOutOrderNo(alipayResp.getOutTradeNo());
            } else if ("alipay_page".equals(method)) {
                AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
                AlipayTradePagePayModel model = new AlipayTradePagePayModel();
                model.setOutTradeNo(req.getBizOrderNo());
                model.setTotalAmount(amount);
                model.setSubject(req.getSubject());
                model.setBody(req.getDescription());
                model.setProductCode("FAST_INSTANT_TRADE_PAY");
                request.setBizModel(model);
                if (StrUtil.isNotBlank(req.getExpireTime())) model.setTimeExpire(req.getExpireTime());
                AlipayTradePagePayResponse alipayResp = client.pageExecute(request);
                resp.setPayBody(alipayResp.getBody());
                resp.setPayBodyType("form");
                if (StrUtil.isNotBlank(alipayResp.getOutTradeNo())) resp.setOutOrderNo(alipayResp.getOutTradeNo());
            } else if ("alipay_qr".equals(method)) {
                AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
                AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
                model.setOutTradeNo(req.getBizOrderNo());
                model.setTotalAmount(amount);
                model.setSubject(req.getSubject());
                model.setBody(req.getDescription());
                request.setBizModel(model);
                // 异步通知地址由主应用通过 config.notifyUrl 下发, 透传给支付宝
                String notifyUrl = req.getConfig() != null && req.getConfig().get("notifyUrl") != null
                        ? req.getConfig().get("notifyUrl").toString() : null;
                if (StrUtil.isNotBlank(notifyUrl)) request.setNotifyUrl(notifyUrl);
                if (StrUtil.isNotBlank(req.getExpireTime())) model.setTimeExpire(req.getExpireTime());
                AlipayTradePrecreateResponse alipayResp = client.execute(request);
                if (alipayResp.isSuccess()) {
                    resp.setPayBody(alipayResp.getQrCode());
                    resp.setPayBodyType("qr_code");
                    if (StrUtil.isNotBlank(alipayResp.getOutTradeNo())) resp.setOutOrderNo(alipayResp.getOutTradeNo());
                } else {
                    // 业务失败单独抛出, 不被下面的 SDK 异常包装
                    throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(), "channel.error.alipayPreCreateFailed", alipayResp.getSubMsg());
                }
            }
            resp.setComplete(false);
        } catch (ChannelServiceException e) {
            // 业务异常直接透传, 避免被包装成 SDK 调用异常
            throw e;
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
        return resp;
    }
}
