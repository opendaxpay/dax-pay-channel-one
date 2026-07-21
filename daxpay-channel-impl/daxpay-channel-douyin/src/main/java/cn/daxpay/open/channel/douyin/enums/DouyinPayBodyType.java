package cn.daxpay.open.channel.douyin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/// # 抖音支付内容类型
///
/// 标识支付下单返回的 payBody 类型, 主应用据此决定前端调起支付的方式。
@Getter
@AllArgsConstructor
public enum DouyinPayBodyType {
    /// 二维码链接(扫码支付, 前端渲染为二维码)
    QR_CODE("QR_CODE"),
    /// 跳转链接(H5 支付, 前端 location.href)
    LINK("LINK"),
    /// JSAPI 调起参数(JSON, 前端通过 ttcjpay.dypay 等通道桥唤起, 含 appId/timeStamp/nonceStr/package/signType/paySign)
    JSAPI("JSAPI"),
    /// 标识符(APP 支付返回的 prepayId, 由客户端 SDK 唤起)
    IDENTIFIER("IDENTIFIER");

    private final String code;
}
