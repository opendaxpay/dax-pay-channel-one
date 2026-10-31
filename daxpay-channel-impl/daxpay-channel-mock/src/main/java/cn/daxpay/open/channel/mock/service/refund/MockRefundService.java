package cn.daxpay.open.channel.mock.service.refund;

import cn.daxpay.open.channel.mock.req.MockRefundReq;
import cn.daxpay.open.channel.mock.resp.MockRefundResp;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.daxpay.open.channel.mock.util.MockDateUtil;
import cn.daxpay.open.channel.mock.util.MockJsonBuilder;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # Mock 通道退款发起服务
///
/// POST /v3/mock/refund(契约 §六-4): 字段映射 outTradeNo→out_trade_no、outRefundNo→out_refund_no、
/// refundAmount→amount(契约 §七: 网关侧金额字段名恒为 amount)、notifyUrl→notify_url;
/// 响应 refund_id/refund_status/refund_amount/finish_time → camel。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockRefundService {

    private final MockGatewayClient gatewayClient;

    /// 发起退款(按 mock 退款场景规则即时定状态, 未终态交同步/回调纠正)
    public MockRefundResp refund(MockRefundReq req) {
        log.info("Mock 通道退款: outTradeNo={}, outRefundNo={}, refundAmount={}",
                req.getOutTradeNo(), req.getOutRefundNo(), req.getRefundAmount());
        Map<String, Object> body = MockJsonBuilder.create()
                .put("mchid", MockGatewayClient.DEFAULT_MCHID)
                .put("out_trade_no", req.getOutTradeNo())
                .put("out_refund_no", req.getOutRefundNo())
                .put("amount", req.getRefundAmount())
                .put("reason", req.getReason())
                .put("notify_url", req.getNotifyUrl())
                .build();
        JSONObject response = gatewayClient.post(req.getCredential(), MockGatewayClient.DEFAULT_MCHID,
                "/v3/mock/refund", body);
        return toResp(response);
    }

    /// 网关 snake 响应 → 内部 camel resp(/refund 与 /refund-sync 共用形态)
    static MockRefundResp toResp(JSONObject response) {
        MockRefundResp resp = new MockRefundResp();
        resp.setOutTradeNo(response.getStr("out_trade_no"));
        resp.setOutRefundNo(response.getStr("out_refund_no"));
        resp.setRefundId(response.getStr("refund_id"));
        resp.setRefundStatus(response.getStr("refund_status"));
        resp.setComplete(response.getBool("complete"));
        resp.setRefundAmount(response.getLong("refund_amount"));
        resp.setFinishTime(MockDateUtil.fromRfc3339(response.getStr("finish_time")));
        return resp;
    }
}
