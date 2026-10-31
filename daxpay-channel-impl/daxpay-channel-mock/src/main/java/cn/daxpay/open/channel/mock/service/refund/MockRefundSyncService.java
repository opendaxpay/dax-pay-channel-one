package cn.daxpay.open.channel.mock.service.refund;

import cn.daxpay.open.channel.mock.req.MockRefundSyncReq;
import cn.daxpay.open.channel.mock.resp.MockRefundResp;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # Mock 通道退款同步服务
///
/// GET /v3/mock/refund/{outRefundNo}?mchid={mchid}(契约 §六-5), 响应形态与 /refund 共用,
/// snake→camel 转换复用 [MockRefundService#toResp]。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockRefundSyncService {

    private final MockGatewayClient gatewayClient;

    /// 退款同步(查询 mock 退款单状态)
    public MockRefundResp sync(MockRefundSyncReq req) {
        log.info("Mock 通道退款同步: outRefundNo={}", req.getOutRefundNo());
        JSONObject response = gatewayClient.get(req.getCredential(), MockGatewayClient.DEFAULT_MCHID,
                "/v3/mock/refund/" + req.getOutRefundNo(), Map.of());
        return MockRefundService.toResp(response);
    }
}
