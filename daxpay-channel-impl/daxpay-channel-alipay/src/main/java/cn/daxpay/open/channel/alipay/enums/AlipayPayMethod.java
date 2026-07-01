package cn.daxpay.open.channel.alipay.enums;

/// # 支付宝支付方式
///
/// 对应支付宝开放平台不同支付场景, 子应用据此选择 SDK 请求类型与 `productCode`。
public enum AlipayPayMethod {
    /// 手机网站支付(`QUICK_WAP_WAY`)
    WAP,
    /// APP 支付(`QUICK_MSECURITY_PAY`)
    APP,
    /// 电脑网站支付(`FAST_INSTANT_TRADE_PAY`)
    PC,
    /// 扫码预下单(预生成二维码, precreate 接口)
    QR,
    /// 付款码支付(当面付, `bar_code` 场景, 同步扣款)
    BARCODE,
    /// 小程序/JSAPI 支付(`JSAPI_PAY`, `alipay.trade.create` 接口)
    JSAPI;
}
