package cn.daxpay.open.channel.wechat.resp;

import lombok.Data;

import java.time.OffsetDateTime;

/// # 微信通道退款响应
///
/// 子应用调用微信 V3 `申请退款` 后回传给主应用。
/// 退款状态 status:
/// - SUCCESS: 退款成功
/// - CLOSED: 退款关闭
/// - PROCESSING: 退款处理中
/// - ABNORMAL: 退款异常
///
/// `complete=false` 表示退款未终态(PROCESSING/ABNORMAL), 需主应用经退款同步查询确认最终状态;
/// `complete=true` 表示退款终态(SUCCESS/CLOSED)。
@Data
public class WechatRefundResp {

    /// 商户订单号(透传 WechatRefundReq.outTradeNo)
    private String outTradeNo;

    /// 微信支付订单号(transaction_id)
    private String transactionId;

    /// 退款单号(透传 WechatRefundReq.outRefundNo)
    private String outRefundNo;

    /// 微信退款单号(refund_id)
    private String refundId;

    /// 退款状态(SUCCESS / CLOSED / PROCESSING / ABNORMAL)
    private String status;

    /// 是否已终态完成(SUCCESS / CLOSED 为 true, 需等待退款资金到账)
    private Boolean complete;

    /// 退款完成时间(success_time, 退款成功时返回)
    private OffsetDateTime finishTime;

    /// 退款金额(refund, 单位: 分)
    private Long refundAmount;

    /// 用户退款金额(payer_refund, 单位: 分)
    private Long payerRefund;
}
