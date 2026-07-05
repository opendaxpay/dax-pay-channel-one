package cn.daxpay.open.channel.alipay.req;

import cn.daxpay.open.channel.alipay.config.AlipaySdkCredential;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/// # 支付宝通道退款请求
///
/// 主应用经声明式 HTTP 客户端下发, 子应用调用 `alipay.trade.refund` 发起退款。
/// outTradeNo 与 tradeNo 至少传一个, 优先使用 tradeNo(支付宝交易号)。
/// outRequestNo 为本次退款请求号, 同一笔订单可多次部分退款, 每次退款 outRequestNo 不可重复。
@Data
public class AlipayRefundReq {

    /// 商户订单号(主应用支付交易号, 对应支付宝 out_trade_no)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String outTradeNo;

    /// 支付宝交易号(trade_no, 下单成功后由支付宝返回, 可选)
    private String tradeNo;

    /// 退款请求号(对应支付宝 out_request_no, 同一订单多次退款每次不可重复)
    @NotBlank(message = "{validation.field.outRequestNo.notBlank}")
    private String outRequestNo;

    /// 退款金额(单位: 分, 子应用调用 SDK 时转为元)
    @NotNull(message = "{validation.field.amount.notNull}")
    @Positive(message = "{validation.field.amount.positive}")
    private Long refundAmount;

    /// 通道调用凭证
    private AlipaySdkCredential credential;
}
