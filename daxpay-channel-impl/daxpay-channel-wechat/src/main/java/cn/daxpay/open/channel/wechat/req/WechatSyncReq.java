package cn.daxpay.open.channel.wechat.req;

import cn.daxpay.open.channel.wechat.config.WechatSdkCredential;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/// # 微信通道同步请求
///
/// 主应用经声明式 HTTP 客户端下发, 子应用调用微信 V3 `查询订单` 接口查询订单状态。
/// outTradeNo 与 transactionId 至少传一个, 优先使用 transactionId(微信支付订单号)。
@Data
public class WechatSyncReq {

    /// 商户订单号(主应用支付交易号, 对应微信 out_trade_no)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String outTradeNo;

    /// 微信支付订单号(transaction_id, 下单/支付成功后由微信返回, 可选)
    private String transactionId;

    /// 通道调用凭证
    private WechatSdkCredential credential;
}
