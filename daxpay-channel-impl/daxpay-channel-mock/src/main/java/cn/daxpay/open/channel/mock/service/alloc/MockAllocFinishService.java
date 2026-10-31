package cn.daxpay.open.channel.mock.service.alloc;

import cn.daxpay.open.channel.mock.req.MockAllocFinishReq;
import cn.daxpay.open.channel.mock.resp.MockAllocResp;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.daxpay.open.channel.mock.util.MockJsonBuilder;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # Mock 通道分账完结服务
///
/// POST /v3/mock/alloc/finish(契约 §六-7): 完结 = 解冻剩余冻结资金给商户本身, 无接收方明细;
/// 响应形态与发起分账共用, snake→camel 复用 [MockAllocService#toResp]。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockAllocFinishService {

    private final MockGatewayClient gatewayClient;

    /// 发起分账完结
    public MockAllocResp allocFinish(MockAllocFinishReq req) {
        log.info("Mock 通道分账完结: allocNo={}, outTradeNo={}", req.getAllocNo(), req.getOutTradeNo());
        Map<String, Object> body = MockJsonBuilder.create()
                .put("mchid", MockGatewayClient.DEFAULT_MCHID)
                .put("alloc_no", req.getAllocNo())
                .put("out_trade_no", req.getOutTradeNo())
                .build();
        JSONObject response = gatewayClient.post(req.getCredential(), MockGatewayClient.DEFAULT_MCHID,
                "/v3/mock/alloc/finish", body);
        return MockAllocService.toResp(response);
    }
}
