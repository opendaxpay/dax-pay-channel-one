package cn.daxpay.open.channel.douyin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/// # 抖音通道支付方式
///
/// 抖音支付支持四种支付方式, 每种方式对应独立的 SDK Service 与下单接口。
/// 关单/查单不区分支付方式, 统一使用 [ApiNativePaymentsService]。
@Getter
@AllArgsConstructor
public enum DouyinPayMethod {
    /// 扫码支付(NATIVE, 返回 codeUrl 二维码链接)
    QR("QR"),
    /// 小程序/JSAPI 支付(返回 prepayId, 需传入 openId)
    JSAPI("JSAPI"),
    /// H5 支付(返回 h5Url 跳转链接)
    H5("H5"),
    /// APP 支付(返回 prepayId, 客户端 SDK 唤起)
    APP("APP");

    private final String code;
}
