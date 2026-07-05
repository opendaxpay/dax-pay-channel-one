package cn.daxpay.open.channel.lakala.enums;

/// # 拉卡拉支付内容类型
///
/// 标识 `LakalaPayResp.payBody` 的内容形态, 主应用据此决定如何渲染支付页。
public enum LakalaPayBodyType {
    /// 跳转链接(银联JSAPI redirect_url)
    LINK,
    /// 二维码内容(支付宝/银联扫码 code)
    QR_CODE,
    /// JSAPI/小程序调起参数 JSON(微信/支付宝 JSAPI acc_resp_fields)
    JSAPI,
    /// 通用标识码(支付宝JSAPI prepay_id)
    IDENTIFIER;
}
