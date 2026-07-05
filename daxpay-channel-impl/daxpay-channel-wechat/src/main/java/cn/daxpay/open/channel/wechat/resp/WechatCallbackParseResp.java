package cn.daxpay.open.channel.wechat.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 微信回调验签解析响应(与主应用镜像)
@Data
@Accessors(chain = true)
public class WechatCallbackParseResp {

    /// 回调类型(PAY / REFUND)
    private String tradeType;

    /// 商户订单号
    private String outTradeNo;

    /// 微信交易号(支付回调)
    private String transactionId;

    /// 退款单号(退款回调)
    private String outRefundNo;

    /// 微信退款单号(退款回调)
    private String refundId;

    /// 交易状态(支付回调)
    private String tradeState;

    /// 退款状态(退款回调)
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
