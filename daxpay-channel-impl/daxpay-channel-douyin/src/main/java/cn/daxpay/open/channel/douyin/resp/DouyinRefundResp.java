package cn.daxpay.open.channel.douyin.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 抖音通道退款响应
///
/// 抖音退款申请返回的退款单号与状态。退款状态码:
/// - SUCCESS 退款成功
/// - PROCESSING 退款处理中(需同步查询确认)
/// - CLOSED 退款关闭
/// - ABNORMAL 退款异常
@Data
@Accessors(chain = true)
public class DouyinRefundResp {

    /// 退款单号(回显)
    private String outRefundNo;

    /// 抖音退款单号(refundId)
    private String refundId;

    /// 退款状态(SUCCESS / PROCESSING / CLOSED / ABNORMAL)
    private String refundStatus;

    /// 退款完成时间(RFC3339)
    private String finishTime;
}
