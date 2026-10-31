package cn.daxpay.open.channel.mock.enums;

/// # 支付内容类型
///
/// 与 mock 子应用的 channel.enums.MockPayBodyType 镜像, 跨 HTTP 传输时按枚举名(name)序列化对齐。
///
/// 批3 起 payBody 形态按支付方式分化: QR/SCAN/H5/PC/APP→URL(收款确认页地址; APP 批6 起与 H5 同址);
/// MP/JSAPI/MINI_APP→JSON(拟真 prepay 结构);
/// BARCODE 即时判定无支付内容。
public enum MockPayBodyType {

    /// 跳转链接(收款确认页地址, 前端可直接打开或渲染成二维码)
    URL,
    /// 拉起参数串(APP 方式历史形态, 如 mock://pay?tradeNo=xxx&prepayId=xxx)
    ///
    /// 批6 起 mock 子应用 APP 方式改与 H5 同址(URL), 本值不再产生; 保留枚举值仅维持镜像一致。
    PARAMS,
    /// JSON 结构(JSAPI/公众号/小程序方式, 拟真 prepay 结构)
    JSON,
    ;
}
