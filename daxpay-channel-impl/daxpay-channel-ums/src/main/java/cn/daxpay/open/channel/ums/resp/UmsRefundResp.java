package cn.daxpay.open.channel.ums.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 银联商务通道退款响应
///
/// 银联商务退款状态码:
/// - SUCCESS 退款成功
/// - PROCESSING 退款处理中(需同步查询确认)
/// - FAIL 退款失败
@Data
@Accessors(chain = true)
public class UmsRefundResp {

    /// 退款单号(回显)
    private String outRefundNo;

    /// 退款状态(SUCCESS / PROCESSING / FAIL)
    private String refundStatus;

    /// 退款完成时间(yyyy-MM-dd HH:mm:ss)
    private String finishTime;
}
