package cn.daxpay.open.channel.lakala.resp;

import lombok.Data;

import java.time.OffsetDateTime;

/// # 拉卡拉通道退款查询响应
///
/// refundStatus 取值: SUCCESS(成功) / FAIL(失败) / PROCESSING(处理中)。
@Data
public class LakalaRefundSyncResp {

    /// 商户退款单号
    private String outRefundNo;

    /// 拉卡拉退款交易号
    private String tradeNo;

    /// 退款状态(SUCCESS / FAIL / PROCESSING)
    private String refundStatus;

    /// 退款完成时间
    private OffsetDateTime finishTime;

    /// 原始响应数据(用于对账/排障)
    private String syncData;
}
