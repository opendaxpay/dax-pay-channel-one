package cn.daxpay.open.channel.wechat.resp;

import lombok.Data;

/// # 微信通道关闭响应
///
/// 子应用调用微信 V3 `关闭订单` 后回传。
/// 微信关单成功无业务返回体; 关闭失败且可兜底(订单已关闭/不存在)时由子应用内部消化为成功,
/// 真正失败时抛 [cn.daxpay.open.platform.core.exception.ChannelServiceException](经 DaxResult 透传)。
@Data
public class WechatCloseResp {

    /// 商户订单号(透传 WechatCloseReq.outTradeNo)
    private String outTradeNo;

    /// 微信支付订单号(transaction_id, 透传请求)
    private String transactionId;
}
