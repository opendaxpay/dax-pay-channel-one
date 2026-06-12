package org.dromara.daxpay.channel.alipay.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.daxpay.channel.alipay.config.AlipaySdkConfig;
import org.dromara.daxpay.channel.common.dto.sync.ChannelSyncReq;
import org.dromara.daxpay.channel.common.dto.sync.ChannelSyncResp;
import org.dromara.daxpay.channel.core.exception.SdkCallException;
import org.dromara.daxpay.channel.core.service.ChannelSyncService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service("alipayRefundSyncService")
@RequiredArgsConstructor
public class AlipayRefundSyncService implements ChannelSyncService {

    private final ObjectMapper objectMapper;

    @Override
    public ChannelSyncResp sync(ChannelSyncReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getConfig());
        try {
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + req.getBizOrderNo() + "\",\"out_request_no\":\"" + req.getBizOrderNo() + "\"}");
            AlipayTradeFastpayRefundQueryResponse alipayResp = client.execute(request);
            ChannelSyncResp resp = new ChannelSyncResp();
            resp.setBizOrderNo(req.getBizOrderNo());
            resp.setStatus("success");
            resp.setOutOrderNo(alipayResp.getTradeNo());
            if (alipayResp.getRefundAmount() != null) {
                resp.setAmount(new BigDecimal(alipayResp.getRefundAmount()).multiply(new BigDecimal(100)).longValue());
            }
            resp.setRawResponse(objectMapper.readValue(alipayResp.getBody(), Map.class));
            return resp;
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
