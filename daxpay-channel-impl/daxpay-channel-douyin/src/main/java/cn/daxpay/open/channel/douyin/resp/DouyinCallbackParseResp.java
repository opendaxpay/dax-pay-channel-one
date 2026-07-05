package cn.daxpay.open.channel.douyin.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 抖音回调验签解析响应
///
/// 子应用使用 [com.douyinpay.api.notification.NotificationParser] 验签并解密回调后,
/// 返回结构化业务数据, 主应用据此更新订单/退款单状态。
///
/// 兼容支付与退款两种回调: 通过 tradeType 区分, 支付回调填充支付字段, 退款回调填充退款字段。
@Data
@Accessors(chain = true)
public class DouyinCallbackParseResp {

    /// 回调类型(PAY 支付回调 / REFUND 退款回调)
    private String tradeType;

    /// 商户订单号(主应用支付交易号, 抖音 out_trade_no)
    private String outTradeNo;

    /// 抖音交易号(transactionId, 支付回调)
    private String transactionId;

    /// 退款单号(主应用退款单号, 抖音 out_refund_no, 退款回调)
    private String outRefundNo;

    /// 抖音退款单号(refundId, 退款回调)
    private String refundId;

    /// 交易状态(支付回调: SUCCESS / REFUND / NOTPAY / USERPAYING / CLOSED / PAYERROR)
    private String tradeState;

    /// 退款状态(退款回调: SUCCESS / PROCESSING / CLOSED / ABNORMAL)
    private String refundStatus;

    /// 金额(单位: 分)
    private Long amount;

    /// 成功时间(RFC3339)
    private String successTime;

    /// 买家 openid
    private String openid;

    /// 验签是否通过
    private boolean verified;
}
