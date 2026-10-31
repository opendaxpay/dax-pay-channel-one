package cn.daxpay.open.channel.mock.resp;

import lombok.Data;

import java.time.OffsetDateTime;

/// # Mock 通道同步响应
///
/// 与 mock 子应用 daxpay-mock-channel 的 `MockSyncResp` 镜像, 字段对齐;
/// 金额单位恒为分(Long)。tradeStatus 取值: SUCCESS / FAIL / WAIT_BUYER_PAY / CLOSED,
/// 状态映射由 MockSyncService(主应用侧) 完成。
@Data
public class MockSyncResp {

    /// 商户订单号(透传 Req.outTradeNo)
    private String outTradeNo;

    /// Mock 通道交易号
    private String tradeNo;

    /// 交易状态(SUCCESS / FAIL / WAIT_BUYER_PAY / CLOSED)
    private String tradeStatus;

    /// 订单金额(单位: 分)
    private Long amount;

    /// 终态时间(成功/失败时返回)
    private OffsetDateTime finishTime;

    /// 实际付款钱包(聚合一码多付: WECHAT/ALIPAY/UNION/DOUYIN; 分钱包单与聚合未选择钱包为 null)
    ///
    /// 取自 10400 查询回执的 provider(buyer_pay_type 语义), 主应用侧回填订单/资金凭证 provider。
    private String provider;
}
