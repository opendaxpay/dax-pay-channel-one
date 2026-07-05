package cn.daxpay.open.channel.douyin.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 抖音通道退款同步响应
@Data
@Accessors(chain = true)
public class DouyinRefundSyncResp {

    /// 退款单号(回显)
    private String outRefundNo;

    /// 抖音退款单号(refundId)
    private String refundId;

    /// 退款状态(SUCCESS / PROCESSING / CLOSED / ABNORMAL)
    private String refundStatus;

    /// 退款金额(单位: 分)
    private Long refundAmount;

    /// 退款完成时间(RFC3339)
    private String finishTime;

    /// 查询失败时的错误码
    private String errorCode;

    /// 查询失败时的错误信息
    private String errorMsg;
}
