package cn.daxpay.open.channel.alipay.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.common.dto.refund.ChannelRefundReq;
import cn.daxpay.open.channel.common.dto.refund.ChannelRefundResp;
import cn.daxpay.open.channel.core.exception.SdkCallException;
import cn.daxpay.open.channel.core.service.ChannelRefundService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service("alipayRefundService")
public class AlipayRefundService implements ChannelRefundService {

    @Override
    public ChannelRefundResp refund(ChannelRefundReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getConfig());
        String amount = new BigDecimal(req.getAmount()).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP).toPlainString();
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            String bizContent = "{" +
                    "\"out_trade_no\":\"" + req.getBizOrderNo() + "\"," +
                    "\"refund_amount\":" + amount + "," +
                    "\"out_request_no\":\"" + req.getBizRefundOrderNo() + "\"" +
                    (req.getReason() != null ? ",\"refund_reason\":\"" + req.getReason() + "\"" : "") +
                    "}";
            request.setBizContent(bizContent);
            AlipayTradeRefundResponse alipayResp = client.execute(request);
            ChannelRefundResp resp = new ChannelRefundResp();
            resp.setBizRefundOrderNo(req.getBizRefundOrderNo());
            if (alipayResp.isSuccess()) {
                resp.setOutRefundOrderNo(alipayResp.getTradeNo());
                resp.setComplete("Y".equals(alipayResp.getFundChange()));
            } else {
                throw new RuntimeException("支付宝退款失败: " + alipayResp.getSubMsg());
            }
            return resp;
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
