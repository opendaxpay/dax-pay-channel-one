package cn.daxpay.open.channel.mock.service.alloc;

import cn.daxpay.open.channel.mock.req.MockAllocReq;
import cn.daxpay.open.channel.mock.resp.MockAllocResp;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.daxpay.open.channel.mock.util.MockDateUtil;
import cn.daxpay.open.channel.mock.util.MockJsonBuilder;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// # Mock 通道分账发起服务
///
/// POST /v3/mock/alloc(契约 §六-6): allocNo→alloc_no、outTradeNo→out_trade_no、
/// receivers 明细逐项 snake 同名(account/name/amount); 响应 out_alloc_no/finish_time → camel。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockAllocService {

    private final MockGatewayClient gatewayClient;

    /// 发起分账(按 mock 分账场景规则即时定状态, 处理中交同步/GUI 人工推进)
    public MockAllocResp alloc(MockAllocReq req) {
        log.info("Mock 通道分账: allocNo={}, outTradeNo={}, receivers={}",
                req.getAllocNo(), req.getOutTradeNo(), req.getReceivers() == null ? 0 : req.getReceivers().size());
        List<Map<String, Object>> receivers = new ArrayList<>();
        if (req.getReceivers() != null) {
            for (MockAllocReq.Receiver receiver : req.getReceivers()) {
                receivers.add(MockJsonBuilder.create()
                        .put("account", receiver.getAccount())
                        .put("name", receiver.getName())
                        .put("amount", receiver.getAmount())
                        .build());
            }
        }
        Map<String, Object> body = MockJsonBuilder.create()
                .put("mchid", MockGatewayClient.DEFAULT_MCHID)
                .put("alloc_no", req.getAllocNo())
                .put("out_trade_no", req.getOutTradeNo())
                .put("receivers", receivers)
                .build();
        JSONObject response = gatewayClient.post(req.getCredential(), MockGatewayClient.DEFAULT_MCHID,
                "/v3/mock/alloc", body);
        return toResp(response);
    }

    /// 网关 snake 响应 → 内部 camel resp(发起/完结/同步共用形态)
    static MockAllocResp toResp(JSONObject response) {
        return new MockAllocResp()
                .setAllocNo(response.getStr("alloc_no"))
                .setOutAllocNo(response.getStr("out_alloc_no"))
                .setStatus(response.getStr("status"))
                .setFinish(response.getBool("finish", false))
                .setFinishTime(MockDateUtil.fromRfc3339(response.getStr("finish_time")));
    }
}
