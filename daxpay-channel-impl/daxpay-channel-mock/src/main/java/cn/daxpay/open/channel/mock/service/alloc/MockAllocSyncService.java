package cn.daxpay.open.channel.mock.service.alloc;

import cn.daxpay.open.channel.mock.req.MockAllocSyncReq;
import cn.daxpay.open.channel.mock.resp.MockAllocResp;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # Mock 通道分账同步服务
///
/// GET /v3/mock/alloc/{allocNo}?mchid={mchid}(契约 §六-8), 响应形态与发起分账共用,
/// snake→camel 转换复用 [MockAllocService#toResp]。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockAllocSyncService {

    private final MockGatewayClient gatewayClient;

    /// 分账同步(查询 mock 分账单状态)
    public MockAllocResp allocSync(MockAllocSyncReq req) {
        log.info("Mock 通道分账同步: allocNo={}", req.getAllocNo());
        JSONObject response = gatewayClient.get(req.getCredential(), MockGatewayClient.DEFAULT_MCHID,
                "/v3/mock/alloc/" + req.getAllocNo(), Map.of());
        return MockAllocService.toResp(response);
    }
}
