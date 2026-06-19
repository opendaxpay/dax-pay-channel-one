package cn.daxpay.open.channel.alipay.service;

import com.alipay.api.internal.util.AlipaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.common.dto.callback.ChannelCallbackVerifyReq;
import cn.daxpay.open.channel.common.dto.callback.ChannelCallbackVerifyResp;
import cn.daxpay.open.channel.core.service.ChannelCallbackVerifyService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service("alipayCallbackVerifyService")
@RequiredArgsConstructor
public class AlipayCallbackVerifyService implements ChannelCallbackVerifyService {

    private final ObjectMapper objectMapper;

    @Override
    public ChannelCallbackVerifyResp verify(ChannelCallbackVerifyReq req) {
        ChannelCallbackVerifyResp resp = new ChannelCallbackVerifyResp();
        try {
            Map<String, String> params = req.getRawParams();
            Map<String, Object> config = req.getConfig();
            String alipayPublicKey = config.get("alipayPublicKey") != null ? config.get("alipayPublicKey").toString() : "";
            boolean verified = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");
            resp.setVerified(verified);
            if (verified) {
                resp.setBizOrderNo(params.get("out_trade_no"));
                resp.setOutOrderNo(params.get("trade_no"));
                String amount = params.get("buyer_pay_amount");
                if (amount != null) {
                    resp.setAmount(new BigDecimal(amount).multiply(new BigDecimal(100)).longValue());
                }
                resp.setFinishTime(params.get("gmt_payment"));
                resp.setBuyerId(params.get("buyer_id"));
                String tradeStatus = params.get("trade_status");
                resp.setStatus("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus) ? "success" : "fail");
                resp.setRawData(objectMapper.convertValue(params, Map.class));
            } else {
                resp.setStatus("fail");
                log.warn("支付宝回调验签失败: bizOrderNo={}", params.get("out_trade_no"));
            }
        } catch (Exception e) {
            log.error("支付宝回调验签异常", e);
            resp.setVerified(false);
            resp.setStatus("fail");
        }
        return resp;
    }
}
