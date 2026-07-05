package cn.daxpay.open.channel.ums.req;

import cn.daxpay.open.channel.ums.config.UmsSdkCredential;
import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import lombok.Data;

/// # 银联商务通道关闭订单请求
///
/// 扫码关单需要 qrCodeId(从支付返回的 billQRCode 链接末段提取),
/// H5 关单需要 merOrderId(即商户订单号)。通过 [method] 区分。
@Data
public class UmsCloseReq {

    /// 商户订单号(扫码关单时用于关联, H5 关单时作为 merOrderId)
    private String outTradeNo;

    /// 二维码 ID(扫码关单必填, 从 billQRCode 链接末段提取)
    private String qrCodeId;

    /// 支付方式(区分扫码关单 / H5 关单)
    private UmsPayMethod method;

    /// 通道调用凭证
    private UmsSdkCredential credential;
}
