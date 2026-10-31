package cn.daxpay.open.channel.mock.service.alloc;

import cn.daxpay.open.channel.mock.req.MockAllocReceiverReq;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.daxpay.open.channel.mock.util.MockJsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # Mock 通道分账接收方解绑服务
///
/// POST /v3/mock/alloc/receivers/unbind(契约 §六-10, 解绑不用 DELETE 带 body 改 POST 独立路径):
/// mock 零资料接入恒成功仅留痕, 无业务响应体。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockAllocReceiverUnbindService {

    private final MockGatewayClient gatewayClient;

    /// 解绑分账接收方
    public void unbind(MockAllocReceiverReq req) {
        log.info("Mock 通道分账接收方解绑: account={}", req.getAccount());
        Map<String, Object> body = MockJsonBuilder.create()
                .put("mchid", MockGatewayClient.DEFAULT_MCHID)
                .put("account", req.getAccount())
                .put("name", req.getName())
                .build();
        gatewayClient.postForVoid(req.getCredential(), MockGatewayClient.DEFAULT_MCHID,
                "/v3/mock/alloc/receivers/unbind", body);
    }
}
