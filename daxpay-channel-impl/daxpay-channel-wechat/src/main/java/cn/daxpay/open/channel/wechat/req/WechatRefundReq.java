package cn.daxpay.open.channel.wechat.req;

import cn.daxpay.open.channel.wechat.config.WechatSdkCredential;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/// # 微信通道退款请求
///
/// 主应用经声明式 HTTP 客户端下发, 子应用调用微信 V3 `申请退款` 接口发起退款。
/// outTradeNo 与 transactionId 至少传一个, 优先使用 transactionId(微信支付订单号)。
/// outRefundNo 为本次退款单号, 同一笔订单可多次部分退款, 每次 outRefundNo 不可重复。
/// 微信退款需同时指定原订单总额(totalAmount)与退款金额(refundAmount)。
@Data
public class WechatRefundReq {

    /// 商户订单号(主应用支付交易号, 对应微信 out_trade_no)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String outTradeNo;

    /// 微信支付订单号(transaction_id, 优先使用)
    private String transactionId;

    /// 退款单号(对应微信 out_refund_no, 同一订单多次退款每次不可重复)
    @NotBlank(message = "{validation.field.outRequestNo.notBlank}")
    private String outRefundNo;

    /// 原订单总金额(单位: 分, 微信退款 amount.total)
    @NotNull(message = "{validation.field.amount.notNull}")
    @Positive(message = "{validation.field.amount.positive}")
    private Long totalAmount;

    /// 退款金额(单位: 分, 微信退款 amount.refund)
    @NotNull(message = "{validation.field.amount.notNull}")
    @Positive(message = "{validation.field.amount.positive}")
    private Long refundAmount;

    /// 退款原因(对应微信 reason, 可选)
    private String reason;

    /// 退款异步通知地址(由子应用透传给微信, 可选)
    private String notifyUrl;

    /// 通道调用凭证
    private WechatSdkCredential credential;
}
