package cn.daxpay.open.channel.mock.req;

import cn.daxpay.open.channel.mock.config.MockSdkCredential;
import lombok.Data;

/// # Mock 通道关单请求
///
/// 与 mock 子应用 daxpay-mock-channel 的 `MockCloseReq` 镜像, 字段对齐。
@Data
public class MockCloseReq {

    /// 商户订单号(主应用支付交易号透传)
    private String outTradeNo;

    /// 通道调用凭证(占位空壳)
    private MockSdkCredential credential;
}
