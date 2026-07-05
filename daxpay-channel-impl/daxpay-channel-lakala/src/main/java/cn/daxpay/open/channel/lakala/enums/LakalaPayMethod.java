package cn.daxpay.open.channel.lakala.enums;

/// # 拉卡拉支付方式
///
/// 拉卡拉聚合通道通过两个接口承载所有支付方式:
/// - MICROPAY: 条码支付(付款码被扫), 走 `/v3/labs/trans/micropay`, accountType 由拉卡拉据 authCode 自动识别
/// - PREORDER: 预下单(扫码/JSAPI/APP/小程序), 走 `/v3/labs/trans/preorder`, 需显式传 accountType + transType
public enum LakalaPayMethod {
    /// 条码支付(付款码被扫, /v3/labs/trans/micropay)
    MICROPAY,
    /// 预下单(扫码/JSAPI/APP/小程序, /v3/labs/trans/preorder)
    PREORDER;
}
