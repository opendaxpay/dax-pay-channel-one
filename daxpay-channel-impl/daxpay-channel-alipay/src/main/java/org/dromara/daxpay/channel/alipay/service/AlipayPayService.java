package org.dromara.daxpay.channel.alipay.service;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.*;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import lombok.extern.slf4j.Slf4j;
import org.dromara.daxpay.channel.alipay.config.AlipaySdkConfig;
import org.dromara.daxpay.channel.common.dto.pay.ChannelPayReq;
import org.dromara.daxpay.channel.common.dto.pay.ChannelPayResp;
import org.dromara.daxpay.channel.core.exception.SdkCallException;
import org.dromara.daxpay.channel.core.service.ChannelPayService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service("alipayPayService")
public class AlipayPayService implements ChannelPayService {

    @Override
    public ChannelPayResp pay(ChannelPayReq req) {
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
                if (StrUtil.isNotBlank(req.getExpireTime())) model.setTimeExpire(req.getExpireTime());
                AlipayTradePrecreateResponse alipayResp = client.execute(request);
                if (alipayResp.isSuccess()) {
                    resp.setPayBody(alipayResp.getQrCode());
                    resp.setPayBodyType("qr_code");
                    if (StrUtil.isNotBlank(alipayResp.getOutTradeNo())) resp.setOutOrderNo(alipayResp.getOutTradeNo());
                } else {
                    throw new RuntimeException("支付宝预创建订单失败: " + alipayResp.getSubMsg());
                }
            }
            resp.setComplete(false);
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
        return resp;
    }
}
