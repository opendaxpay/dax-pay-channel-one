package cn.daxpay.open.channel.mock.service.close;

import cn.daxpay.open.channel.mock.req.MockCloseReq;
import cn.daxpay.open.channel.mock.resp.MockCloseResp;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # Mock 通道关单服务
///
/// POST /v3/mock/pay/{outTradeNo}/close, 请求体 ``{mchid}``(契约 §六-3):
/// 内部 DTO 无商户标识, mchid 取缺省常量; 响应 out_trade_no/trade_no/closed → camel。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockCloseService {

    private final MockGatewayClient gatewayClient;

    /// 关闭 mock 订单
    public MockCloseResp close(MockCloseReq req) {
        log.info("Mock 通道关单: outTradeNo={}", req.getOutTradeNo());
        JSONObject response = gatewayClient.post(req.getCredential(), MockGatewayClient.DEFAULT_MCHID,
                "/v3/mock/pay/" + req.getOutTradeNo() + "/close",
                Map.of("mchid", MockGatewayClient.DEFAULT_MCHID));
        MockCloseResp resp = new MockCloseResp();
        resp.setOutTradeNo(response.getStr("out_trade_no"));
        resp.setTradeNo(response.getStr("trade_no"));
        resp.setClosed(response.getBool("closed"));
        return resp;
    }
}
