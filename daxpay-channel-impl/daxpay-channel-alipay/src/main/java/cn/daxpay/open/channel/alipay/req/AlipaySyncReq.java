package cn.daxpay.open.channel.alipay.req;

import cn.daxpay.open.channel.alipay.config.AlipaySdkCredential;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/// # 支付宝通道同步请求
///
/// 主应用经声明式 HTTP 客户端下发, 子应用调用 `alipay.trade.query` 查询订单状态。
/// outTradeNo 与 tradeNo 至少传一个, 优先使用 tradeNo(支付宝交易号)。
@Data
public class AlipaySyncReq {

    /// 商户订单号(主应用支付交易号, 对应支付宝 out_trade_no)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String outTradeNo;

    /// 支付宝交易号(trade_no, 下单成功后由支付宝返回, 可选)
    private String tradeNo;

    /// 通道调用凭证
    private AlipaySdkCredential credential;
}
