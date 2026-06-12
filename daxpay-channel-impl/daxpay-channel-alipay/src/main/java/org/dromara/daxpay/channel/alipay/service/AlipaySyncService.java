package org.dromara.daxpay.channel.alipay.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
@Service("alipaySyncService")
@RequiredArgsConstructor
public class AlipaySyncService implements ChannelSyncService {

    private final ObjectMapper objectMapper;

    @Override
    public ChannelSyncResp sync(ChannelSyncReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getConfig());
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + req.getBizOrderNo() + "\"}");
            AlipayTradeQueryResponse alipayResp = client.execute(request);
            ChannelSyncResp resp = new ChannelSyncResp();
            resp.setBizOrderNo(req.getBizOrderNo());
            if (alipayResp.isSuccess()) {
                resp.setOutOrderNo(alipayResp.getTradeNo());
                resp.setStatus(mapStatus(alipayResp.getTradeStatus()));
                if (alipayResp.getTotalAmount() != null) {
                    resp.setAmount(new BigDecimal(alipayResp.getTotalAmount()).multiply(new BigDecimal(100)).longValue());
                }
                if (alipayResp.getSendPayDate() != null) {
                    resp.setFinishTime(alipayResp.getSendPayDate().toString());
                }
                resp.setBuyerId(alipayResp.getBuyerUserId());
            } else {
                resp.setStatus("ACQ.TRADE_NOT_EXIST".equals(alipayResp.getSubCode()) ? "not_exist" : "fail");
            }
            resp.setRawResponse(objectMapper.readValue(alipayResp.getBody(), Map.class));
            return resp;
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }

    private String mapStatus(String tradeStatus) {
        if (tradeStatus == null) return "fail";
        return switch (tradeStatus) {
            case "TRADE_SUCCESS", "TRADE_FINISHED" -> "success";
            case "WAIT_BUYER_PAY" -> "processing";
            case "TRADE_CLOSED" -> "close";
            default -> "fail";
        };
    }
}
