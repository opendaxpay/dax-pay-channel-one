package org.dromara.daxpay.channel.alipay.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.daxpay.channel.alipay.config.AlipaySdkConfig;
import org.dromara.daxpay.channel.common.dto.close.ChannelCloseReq;
import org.dromara.daxpay.channel.common.dto.close.ChannelCloseResp;
import org.dromara.daxpay.channel.core.exception.SdkCallException;
import org.dromara.daxpay.channel.core.service.ChannelCloseService;
import org.springframework.stereotype.Service;

@Slf4j
@Service("alipayCloseService")
public class AlipayCloseService implements ChannelCloseService {

    @Override
    public ChannelCloseResp close(ChannelCloseReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getConfig());
        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            request.setBizContent("{\"out_trade_no\":\"" + req.getBizOrderNo() + "\"}");
            AlipayTradeCloseResponse alipayResp = client.execute(request);
            ChannelCloseResp resp = new ChannelCloseResp();
            resp.setBizOrderNo(req.getBizOrderNo());
            resp.setResult(alipayResp.isSuccess() ? "close_success" : "close_fail");
            return resp;
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
