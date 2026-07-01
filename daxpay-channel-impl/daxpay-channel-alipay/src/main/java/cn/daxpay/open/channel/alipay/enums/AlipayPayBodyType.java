package cn.daxpay.open.channel.alipay.enums;

/// # 支付内容类型
///
/// 标识 `AlipayPayResp.payBody` 的内容形态, 主应用据此决定如何渲染支付页。
public enum AlipayPayBodyType {
    /// 跳转链接(wap / pc 场景, 由支付宝 pageExecute(GET) 生成, 前端可直接 location.href 跳转)
    LINK,
    /// 二维码内容(qr 扫码场景, 前端渲染成二维码图片)
    QR_CODE,
    /// APP 订单串(app 场景, 透传给客户端 SDK 拉起支付宝)
    ORDER_STR,
    /// 标识符(jsapi 场景, 返回支付宝交易号 tradeNo, 供小程序 SDK 调起支付)
    IDENTIFIER;
}
