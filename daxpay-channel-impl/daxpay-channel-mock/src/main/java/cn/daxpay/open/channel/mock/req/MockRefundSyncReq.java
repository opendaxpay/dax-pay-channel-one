package cn.daxpay.open.channel.mock.req;

import cn.daxpay.open.channel.mock.config.MockSdkCredential;
import lombok.Data;

/// # Mock 通道退款同步请求
///
/// 与 mock 子应用 daxpay-mock-channel 的 `MockRefundSyncReq` 镜像, 字段对齐。
@Data
public class MockRefundSyncReq {

    /// 商户退款号(透传 MockRefundReq.outRefundNo)
    private String outRefundNo;

    /// 通道调用凭证(占位空壳)
    private MockSdkCredential credential;
}
