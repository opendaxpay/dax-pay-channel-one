package cn.daxpay.open.channel.wechat.enums;

/// # 微信支付方式
///
/// 对应微信支付 V3 不同下单场景, 子应用据此选择 [com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum] 与 SDK 下单方法。
/// 小程序(MINI)与公众号(JSAPI)在微信侧同走 JSAPI 接口, 仅 appId 来源不同。
/// 付款码(MICROPAY)走 V3 `codepay` 接口(同步扣款)。
public enum WechatPayMethod {
    /// 扫码支付(NATIVE, 返回 code_url)
    NATIVE,
    /// 公众号支付(JSAPI, 返回 prepay_id + 调起参数)
    JSAPI,
    /// 小程序支付(JSAPI 接口, 返回 prepay_id + 调起参数)
    MINI,
    /// APP 支付(APP, 返回 prepay_id + APP 调起参数)
    APP,
    /// H5 支付(H5, 返回 h5_url)
    H5,
    /// 付款码支付(当面付, V3 codepay 接口, 同步扣款)
    MICROPAY;
}
