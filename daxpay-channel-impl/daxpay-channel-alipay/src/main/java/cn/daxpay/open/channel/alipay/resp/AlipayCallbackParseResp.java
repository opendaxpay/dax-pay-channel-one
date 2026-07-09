package cn.daxpay.open.channel.alipay.resp;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

/// # 支付宝回调验签解析响应
///
/// 子应用使用 [com.alipay.api.internal.util.AlipaySignature] 验签通过后,
/// 将标准化业务字段回传主应用。主应用据此更新支付单/退款单状态。
///
/// 兼容支付与退款两种回调: 通过 tradeType 区分, 支付回调填充支付字段, 退款回调填充退款字段。
@Data
@Accessors(chain = true)
public class AlipayCallbackParseResp {

    /// 是否验签通过
    private Boolean success;

    /// 回调类型(PAY 支付回调 / REFUND 退款回调)
    private String tradeType;

    /// 商户订单号(主应用支付交易号, 支付宝 out_trade_no)或商户退款单号(退款回调 out_request_no)
    private String outTradeNo;

    /// 支付宝交易号(trade_no, 支付回调)
    private String tradeNo;

    /// 支付宝退款流水号(refund_detail_id 或 trade_no, 退款回调)
    private String outRefundNo;

    /// 交易状态(抽象态 SUCCESS / FAIL, 已屏蔽支付宝 trade_status / refund_status 原始码)
    private String tradeStatus;

    /// 金额(单位: 分, total_amount / refund_amount 元转分)
    private Long amount;

    /// 完成时间(gmt_payment / gmt_refund 解析为东八区 OffsetDateTime)
    private OffsetDateTime finishTime;
}
