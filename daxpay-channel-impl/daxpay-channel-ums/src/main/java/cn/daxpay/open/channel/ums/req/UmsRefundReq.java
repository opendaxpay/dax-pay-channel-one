package cn.daxpay.open.channel.ums.req;

import cn.daxpay.open.channel.ums.config.UmsSdkCredential;
import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import lombok.Data;

import java.time.OffsetDateTime;

/// # 银联商务通道退款请求
///
/// 扫码退款与 H5 退款字段名不同(扫码用 billNo, H5 用 merOrderId),
/// 通过 [#method] 区分, 子应用据此选择对应接口。
@Data
public class UmsRefundReq {

    /// 原商户订单号(扫码退款作为 billNo, H5 退款作为 merOrderId)
    private String outTradeNo;

    /// 原订单创建时间(UTC, 主应用传入), 银联商务要求东八区 yyyy-MM-dd, 由 [UmsDateUtil] 转换
    private OffsetDateTime billDate;

    /// 退款单号(主应用退款单号, 作为银联 refundOrderId)
    private String outRefundNo;

    /// 退款金额(单位: 分)
    private Long refundAmount;

    /// 退款原因
    private String reason;

    /// 退款异步通知地址(H5 退款使用)
    private String notifyUrl;

    /// 支付方式(区分扫码退款 / H5 退款)
    private UmsPayMethod method;

    /// 通道调用凭证
    private UmsSdkCredential credential;
}
