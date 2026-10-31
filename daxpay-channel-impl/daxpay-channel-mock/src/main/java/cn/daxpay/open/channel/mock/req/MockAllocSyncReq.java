package cn.daxpay.open.channel.mock.req;

import cn.daxpay.open.channel.mock.config.MockSdkCredential;
import lombok.Data;
import lombok.experimental.Accessors;

/// # Mock 通道分账同步请求
///
/// 与 mock 子应用 daxpay-mock-channel 的 `MockAllocSyncReq` 镜像, 字段对齐。
@Data
@Accessors(chain = true)
public class MockAllocSyncReq {

    /// 分账单号(平台分账单号 allocNo 透传)
    private String allocNo;

    /// 通道调用凭证(占位空壳)
    private MockSdkCredential credential;
}
