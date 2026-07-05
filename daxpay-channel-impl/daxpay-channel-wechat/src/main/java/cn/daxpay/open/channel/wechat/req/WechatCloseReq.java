package cn.daxpay.open.channel.wechat.req;

import cn.daxpay.open.channel.wechat.config.WechatSdkCredential;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/// # 微信通道关闭请求
///
/// 主应用经声明式 HTTP 客户端下发, 子应用调用微信 V3 `关闭订单` 接口终结未支付订单。
/// outTradeNo 与 transactionId 至少传一个; 微信关单仅支持 out_trade_no, 子应用内部按需查询 transactionId 对应的 outTradeNo。
@Data
public class WechatCloseReq {

    /// 商户订单号(主应用支付交易号, 对应微信 out_trade_no)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String outTradeNo;

    /// 微信支付订单号(transaction_id, 可选)
    private String transactionId;

    /// 通道调用凭证
    private WechatSdkCredential credential;
}
