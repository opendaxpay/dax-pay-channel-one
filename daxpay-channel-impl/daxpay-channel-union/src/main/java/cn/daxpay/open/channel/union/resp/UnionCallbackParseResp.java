package cn.daxpay.open.channel.union.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 云闪付回调验签解析响应
///
/// 子应用使用 [cn.daxpay.open.channel.union.util.UnionSignUtil] 完成证书验签后,
/// 将回调参数解析为结构化业务数据返回主应用。
///
/// 兼容支付与退款两种回调: 通过 tradeType 区分。
/// 统一状态码与 [UnionSyncResp] / [UnionRefundSyncResp] 一致。
@Data
@Accessors(chain = true)
public class UnionCallbackParseResp {

    /// 验签是否通过
    private boolean verified;

    /// 回调类型(PAY 支付回调 / REFUND 退款回调)
    private String tradeType;

    /// 商户订单号(支付回调: orderId; 退款回调: 原订单号)
    private String outTradeNo;

    /// 退款单号(退款回调: orderId; 支付回调为空)
    private String outRefundNo;

    /// 统一交易状态(SUCCESS / PROGRESS / CLOSED)
    private String tradeStatus;

    /// 金额(单位: 分, 支付回调为订单金额 txnAmt, 退款回调为退款金额)
    private Long amount;

    /// 实付/实退金额(单位: 分)
    private Long realAmount;

    /// 支付/退款完成时间(yyyyMMddHHmmss, 东八区)
    private String finishTime;

    /// 银联交易查询凭证(支付回调必填, 退款时作为 origQryId)
    private String queryId;

    /// 买家标识
    private String buyerId;
}
