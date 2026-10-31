package cn.daxpay.open.channel.mock.service.sync;

import cn.daxpay.open.channel.mock.req.MockSyncReq;
import cn.daxpay.open.channel.mock.resp.MockSyncResp;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.daxpay.open.channel.mock.util.MockDateUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # Mock 通道支付同步服务
///
/// GET /v3/mock/pay/{outTradeNo}?mchid={mchid}(契约 §六-2): 内部 DTO 无商户标识, mchid 取缺省常量;
/// 响应 trade_status/finish_time(snake, RFC3339)→ tradeStatus/finishTime(camel, OffsetDateTime)。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockSyncService {

    private final MockGatewayClient gatewayClient;

    /// 支付同步(查询 mock 订单状态)
    public MockSyncResp sync(MockSyncReq req) {
        log.info("Mock 通道支付同步: outTradeNo={}", req.getOutTradeNo());
        JSONObject response = gatewayClient.get(req.getCredential(), MockGatewayClient.DEFAULT_MCHID,
                "/v3/mock/pay/" + req.getOutTradeNo(), Map.of());
        MockSyncResp resp = new MockSyncResp();
        resp.setOutTradeNo(response.getStr("out_trade_no"));
        resp.setTradeNo(response.getStr("trade_no"));
        resp.setTradeStatus(response.getStr("trade_status"));
        resp.setAmount(response.getLong("amount"));
        resp.setFinishTime(MockDateUtil.fromRfc3339(response.getStr("finish_time")));
        // 聚合一码多付: 实际付款钱包透传(10400 查询回执 provider, 主应用据此回填资金凭证/容器)
        resp.setProvider(response.getStr("provider"));
        return resp;
    }
}
