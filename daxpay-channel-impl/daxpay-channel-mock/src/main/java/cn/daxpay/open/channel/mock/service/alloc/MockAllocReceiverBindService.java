package cn.daxpay.open.channel.mock.service.alloc;

import cn.daxpay.open.channel.mock.req.MockAllocReceiverReq;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.daxpay.open.channel.mock.util.MockJsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # Mock 通道分账接收方绑定服务
///
/// POST /v3/mock/alloc/receivers(契约 §六-9): mock 零资料接入恒成功仅留痕, 无业务响应体。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockAllocReceiverBindService {

    private final MockGatewayClient gatewayClient;

    /// 绑定分账接收方
    public void bind(MockAllocReceiverReq req) {
        log.info("Mock 通道分账接收方绑定: account={}", req.getAccount());
        Map<String, Object> body = MockJsonBuilder.create()
                .put("mchid", MockGatewayClient.DEFAULT_MCHID)
                .put("account", req.getAccount())
                .put("name", req.getName())
                .build();
        gatewayClient.postForVoid(req.getCredential(), MockGatewayClient.DEFAULT_MCHID,
                "/v3/mock/alloc/receivers", body);
    }
}
