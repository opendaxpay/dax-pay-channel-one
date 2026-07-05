package cn.daxpay.open.channel.alipay.resp;

import lombok.Data;

import java.time.OffsetDateTime;

/// # 支付宝通道退款同步响应
///
/// 子应用调用 `alipay.trade.fastpay.refund.query` 后原样回传字段, 不做业务状态映射。
/// refund_status 映射由主应用完成。
@Data
public class AlipayRefundSyncResp {

    /// 退款状态(REFUND_SUCCESS=退款成功, 空=未查询到/处理中)
    private String refundStatus;

    /// 网关返回码(code, 10000=成功)
    private String code;

    /// 业务返回码(sub_code)
    private String subCode;

    /// 业务返回消息(sub_msg)
    private String subMsg;

    /// 商户订单号(out_trade_no, 透传请求)
    private String outTradeNo;

    /// 支付宝交易号(trade_no)
    private String tradeNo;

    /// 退款请求号(out_request_no, 透传请求)
    private String outRequestNo;

    /// 退款完成时间(gmt_refund_pay)
    private OffsetDateTime finishTime;

    /// 退款金额(refund_amount, 单位: 分, 由元转分)
    private Long refundAmount;
}
