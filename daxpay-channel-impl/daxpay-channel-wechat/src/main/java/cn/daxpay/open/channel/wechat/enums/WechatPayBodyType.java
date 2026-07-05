package cn.daxpay.open.channel.wechat.enums;

/// # 微信支付内容类型
///
/// 标识 `WechatPayResp.payBody` 的内容形态, 主应用据此决定如何渲染支付页。
public enum WechatPayBodyType {
    /// 跳转链接(H5 场景, 微信 h5_url, 前端可直接 location.href 跳转)
    LINK,
    /// 二维码内容(NATIVE 扫码场景, 微信 code_url, 前端渲染成二维码图片)
    QR_CODE,
    /// JSAPI/小程序调起参数 JSON(含 appId/timeStamp/nonceStr/package/signType/paySign, 透传给公众号/小程序 SDK 调 wx.requestPayment)
    JSAPI,
    /// APP 调起参数 JSON(APP 场景, 含 appid/partnerId/prepayId/package/noncestr/timestamp/sign, 透传给客户端 SDK)
    APP_ORDER_STR,
    /// 通用标识码(兜底, 当前无场景使用)
    IDENTIFIER;
}
