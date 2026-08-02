package cn.daxpay.open.channel.union.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/// # 云闪付支付内容类型
///
/// 标识支付下单返回的 payBody 类型, 主应用据此决定前端调起支付的方式。
@Getter
@AllArgsConstructor
public enum UnionPayBodyType {

    /// 二维码内容(主扫支付, 银联返回 qrNo, 前端按 emv 规则渲染为二维码)
    QR_CODE("QR_CODE"),

    /// 跳转链接(H5 支付, 前端 location.href 跳转银联收银台)
    LINK("LINK");

    private final String code;
}
