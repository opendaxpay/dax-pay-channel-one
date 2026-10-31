package cn.daxpay.open.channel.mock.req;

import cn.daxpay.open.channel.mock.config.MockSdkCredential;
import lombok.Data;
import lombok.experimental.Accessors;

/// # Mock 通道分账接收方绑定/解绑请求
///
/// 与 mock 子应用 daxpay-mock-channel 的 `MockAllocReceiverReq` 镜像, 字段对齐。
/// mock 零资料接入, 绑定/解绑恒成功仅留痕。
@Data
@Accessors(chain = true)
public class MockAllocReceiverReq {

    /// 接收方账号
    private String account;

    /// 接收方名称(可空)
    private String name;

    /// 通道调用凭证(占位空壳)
    private MockSdkCredential credential;
}
