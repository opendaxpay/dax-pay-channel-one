package cn.daxpay.open.channel.ums.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 银联商务回调验签解析响应
///
/// 子应用使用 [cn.daxpay.open.channel.ums.util.UmsSignUtil] 验签后,
/// 将回调参数解析为结构化业务数据返回主应用。
///
/// 兼容支付与退款两种回调: 通过 tradeType 区分, 支付回调填充支付字段, 退款回调填充退款字段。
/// 统一状态码与 [UmsSyncResp] / [UmsRefundSyncResp] 一致。
@Data
@Accessors(chain = true)
public class UmsCallbackParseResp {

    /// 验签是否通过
    private boolean verified;

    /// 回调类型(PAY 支付回调 / REFUND 退款回调)
    private String tradeType;

    /// 商户订单号(支付回调: billNo/merOrderId; 退款回调: 原订单号)
    private String outTradeNo;

    /// 退款单号(退款回调: refundOrderId; 支付回调为空)
    private String outRefundNo;

    /// 统一交易状态(支付: SUCCESS/PROGRESS/CLOSED; 退款: SUCCESS/PROGRESS/CLOSED)
    private String tradeStatus;

    /// 金额(单位: 分, 支付回调为订单金额, 退款回调为退款金额)
    private Long amount;

    /// 实付/实退金额(单位: 分)
    private Long realAmount;

    /// 支付/退款完成时间(yyyy-MM-dd HH:mm:ss)
    private String finishTime;

    /// 买家标识
    private String buyerId;

    /// 支付厂商(Alipay / WXPay / UnionPay / ACP / UAC)
    private String targetSys;

    /// 第三方订单号
    private String targetOrderId;
}
