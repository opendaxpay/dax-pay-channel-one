package cn.daxpay.open.channel.ums.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/// # 银联商务支付内容类型
///
/// 标识支付下单返回的 payBody 类型, 主应用据此决定前端调起支付的方式。
@Getter
@AllArgsConstructor
public enum UmsPayBodyType {

    /// 二维码链接(扫码支付, 前端渲染为二维码)
    QR_CODE("QR_CODE"),

    /// 跳转链接(H5 支付, 前端 location.href 跳转银联商务收银台)
    LINK("LINK");

    private final String code;
}
