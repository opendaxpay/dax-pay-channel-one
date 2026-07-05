package cn.daxpay.open.channel.wechat.enums;

/// # 微信支付内容类型
///
/// 标识 `WechatPayResp.payBody` 的内容形态, 主应用据此决定如何渲染支付页。
public enum WechatPayBodyType {
    /// 跳转链接(H5 场景, 微信 h5_url, 前端可直接 location.href 跳转)
    LINK,
    /// 二维码内容(NATIVE 扫码场景, 微信 code_url, 前端渲染成二维码图片)
    QR_CODE,
    /// 调起参数 JSON(JSAPI / 小程序场景, 含 appId/timeStamp/nonceStr/package/signType/paySign, 透传给小程序 SDK)
    IDENTIFIER,
    /// APP 调起参数 JSON(APP 场景, 含 appid/partnerId/prepayId/package/noncestr/timestamp/sign, 透传给客户端 SDK)
    APP_ORDER_STR;
}
