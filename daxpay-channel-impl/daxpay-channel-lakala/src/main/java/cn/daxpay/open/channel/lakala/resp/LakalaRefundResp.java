package cn.daxpay.open.channel.lakala.resp;

import lombok.Data;

import java.time.OffsetDateTime;

/// # 拉卡拉通道退款响应
///
/// `complete=false` 表示退款处理中(需轮询同步确认); `complete=true` 表示退款同步成功。
@Data
public class LakalaRefundResp {

    /// 商户退款单号(透传 LakalaRefundReq.outRefundNo)
    private String outRefundNo;

    /// 拉卡拉退款交易号(trade_no)
    private String tradeNo;

    /// 是否已完成(有完成时间即视为成功)
    private Boolean complete;

    /// 退款完成时间
    private OffsetDateTime finishTime;
}
