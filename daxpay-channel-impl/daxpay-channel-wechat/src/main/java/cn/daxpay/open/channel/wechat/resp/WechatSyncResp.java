package cn.daxpay.open.channel.wechat.resp;

import lombok.Data;

import java.time.OffsetDateTime;

/// # 微信通道同步响应
///
/// 子应用调用微信 V3 `查询订单` 后原样回传字段, 不做业务状态映射。
/// trade_state 映射由主应用 [cn.daxpay.open.channel.wechat.service.sync.WechatSyncService] 完成。
@Data
public class WechatSyncResp {

    /// 交易状态(SUCCESS / REFUND / NOTPAY / CLOSED / REVOKED / USERPAYING / PAYERROR / ACCEPT)
    private String tradeState;

    /// 交易状态描述
    private String tradeStateDesc;

    /// 微信支付订单号(transaction_id)
    private String transactionId;

    /// 商户订单号(out_trade_no, 透传请求)
    private String outTradeNo;

    /// 支付完成时间(success_time, RFC3339)
    private OffsetDateTime successTime;

    /// 订单总金额(total, 单位: 分, 支付成功时返回)
    private Long totalAmount;

    /// 用户支付金额(payer_total, 单位: 分, 支付成功时返回)
    private Long payerTotal;

    /// 用户标识(openid, 支付成功时返回)
    private String openId;
}
