package cn.daxpay.open.channel.union.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/// # 云闪付通道支付方式
///
/// 云闪付(直连银联 ACP)按支付形态拆分, 一期支持三种:
/// - **QRCODE**: 主扫(申请二维码, C 扫 B), 银联交易类型 APPLY_QR_CODE
/// - **BARCODE**: 被扫(付款码, B 扫 C), 银联交易类型 CONSUME
/// - **H5**: WAP 网关支付(前台跳转银联收银台)
@Getter
@AllArgsConstructor
public enum UnionPayMethod {

    /// 主扫支付(申请二维码, 返回 qrNo, 用户扫码支付)
    QRCODE("QRCODE"),

    /// 被扫支付(付款码消费, 传入用户付款码 qrNo)
    BARCODE("BARCODE"),

    /// H5/WAP 网关支付(返回跳转链接, 浏览器跳转银联收银台)
    H5("H5");

    private final String code;
}
