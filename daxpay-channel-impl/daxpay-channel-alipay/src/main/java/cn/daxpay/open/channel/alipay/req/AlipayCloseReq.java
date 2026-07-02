package cn.daxpay.open.channel.alipay.req;

import cn.daxpay.open.channel.alipay.config.AlipaySdkCredential;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/// # 支付宝通道关闭请求
///
/// 主应用经声明式 HTTP 客户端下发, 子应用调用 `alipay.trade.close`(关闭) 或 `alipay.trade.cancel`(撤销)。
/// outTradeNo 与 tradeNo 至少传一个, 优先使用 tradeNo(支付宝交易号)。
/// 由 useCancel 决定调用方式: false=关闭(默认, 未支付订单), true=撤销(24h 内有效, 需签约权限)。
@Data
public class AlipayCloseReq {

    /// 商户订单号(主应用支付交易号, 对应支付宝 out_trade_no)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String outTradeNo;

    /// 支付宝交易号(trade_no, 下单成功后由支付宝返回, 可选)
    private String tradeNo;

    /// 是否使用撤销方式关闭订单(false=交易关闭, true=交易撤销)
    private boolean useCancel;

    /// 通道调用凭证
    private AlipaySdkCredential credential;
}
