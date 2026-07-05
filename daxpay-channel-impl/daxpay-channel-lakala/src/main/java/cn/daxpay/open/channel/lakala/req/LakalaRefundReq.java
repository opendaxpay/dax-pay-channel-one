package cn.daxpay.open.channel.lakala.req;

import cn.daxpay.open.channel.lakala.config.LakalaSdkCredential;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/// # 拉卡拉通道退款请求
///
/// 原路退款, 支持部分退款。走 `/v3/labs/relation/refund`。
@Data
public class LakalaRefundReq {

    /// 通道调用凭证
    @NotNull(message = "{validation.field.credential.notNull}")
    private LakalaSdkCredential credential;

    /// 商户退款单号(作为拉卡拉 out_trade_no)
    @NotBlank(message = "{validation.field.refundNo.notBlank}")
    private String outRefundNo;

    /// 原商户订单号(原支付 out_trade_no)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String originOutTradeNo;

    /// 原拉卡拉交易号(原支付 trade_no, 与 originOutTradeNo 二选一)
    private String originTradeNo;

    /// 退款金额(单位: 分)
    @NotNull(message = "{validation.field.amount.notNull}")
    @Positive(message = "{validation.field.amount.positive}")
    private Long amount;

    /// 退款原因
    private String reason;

    /// 客户端IP
    private String clientIp;
}
