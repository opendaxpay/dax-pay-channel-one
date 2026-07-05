package cn.daxpay.open.channel.ums.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/// # 银联商务通道支付方式
///
/// 银联商务将不同支付渠道(支付宝/微信/银联)的 H5 与扫码拆为独立接口,
/// 通过本枚举在支付请求中区分, 子应用据此路由到对应 [cn.daxpay.open.channel.ums.sdk.UmsClient] 方法。
@Getter
@AllArgsConstructor
public enum UmsPayMethod {

    /// 扫码支付(主扫, 返回 billQRCode 二维码链接, instMid=QRPAYDEFAULT)
    QRCODE("QRCODE"),

    /// 支付宝 H5 支付(返回跳转链接, instMid=H5DEFAULT)
    ALIPAY_H5("ALIPAY_H5"),

    /// 微信 H5 支付(返回跳转链接, instMid=H5DEFAULT)
    WECHAT_H5("WECHAT_H5"),

    /// 微信小程序收银台支付(H5 转小程序, 返回跳转链接, instMid=H5DEFAULT)
    WECHAT_CASHIER("WECHAT_CASHIER"),

    /// 银联云闪付 H5/JSAPI 支付(返回跳转链接, instMid=H5DEFAULT)
    UNION_JSAPI("UNION_JSAPI");

    private final String code;
}
