package cn.daxpay.open.channel.wechat.resp;

import lombok.Data;

import java.time.OffsetDateTime;

/// # 微信通道退款同步响应
///
/// 子应用调用微信 V3 `查询单笔退款` 后原样回传字段, 不做业务状态映射。
/// refund status 映射由主应用完成。
@Data
public class WechatRefundSyncResp {

    /// 退款状态(SUCCESS / CLOSED / PROCESSING / ABNORMAL)
    private String status;

    /// 微信退款单号(refund_id)
    private String refundId;

    /// 退款单号(out_refund_no, 透传请求)
    private String outRefundNo;

    /// 微信支付订单号(transaction_id)
    private String transactionId;

    /// 商户订单号(out_trade_no)
    private String outTradeNo;

    /// 退款完成时间(success_time)
    private OffsetDateTime finishTime;

    /// 退款金额(refund, 单位: 分)
    private Long refundAmount;

    /// 用户退款金额(payer_refund, 单位: 分)
    private Long payerRefund;
}
