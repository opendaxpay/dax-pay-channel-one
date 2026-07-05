package cn.daxpay.open.channel.douyin.req;

import cn.daxpay.open.channel.douyin.config.DouyinSdkCredential;
import lombok.Data;

/// # 抖音通道退款请求
///
/// 抖音退款需同时传入退款金额与原订单总额, 由 SDK 校验退款不能超过原金额。
@Data
public class DouyinRefundReq {

    /// 原商户订单号(抖音 out_trade_no)
    private String outTradeNo;

    /// 退款单号(主应用退款单号, 作为抖音 out_refund_no)
    private String outRefundNo;

    /// 退款金额(单位: 分)
    private Long refundAmount;

    /// 原订单总金额(单位: 分, 抖音退款接口要求)
    private Long totalAmount;

    /// 退款原因
    private String reason;

    /// 退款异步通知地址
    private String notifyUrl;

    /// 通道调用凭证
    private DouyinSdkCredential credential;
}
