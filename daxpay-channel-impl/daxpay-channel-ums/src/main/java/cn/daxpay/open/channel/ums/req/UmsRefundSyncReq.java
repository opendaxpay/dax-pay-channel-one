package cn.daxpay.open.channel.ums.req;

import cn.daxpay.open.channel.ums.config.UmsSdkCredential;
import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import lombok.Data;

import java.time.OffsetDateTime;

/// # 银联商务通道退款同步请求
///
/// 通过退款单号查询退款最终状态。扫码与 H5 查询接口不同, 通过 [#method] 区分。
@Data
public class UmsRefundSyncReq {

    /// 退款单号(主应用退款单号, 作为银联 refundOrderId)
    private String outRefundNo;

    /// 原商户订单号(扫码退款查询需要)
    private String outTradeNo;

    /// 原订单创建时间(UTC, 主应用传入), 银联商务要求东八区 yyyy-MM-dd, 由 [UmsDateUtil] 转换
    private OffsetDateTime billDate;

    /// 支付方式(区分扫码退款查询 / H5 退款查询)
    private UmsPayMethod method;

    /// 通道调用凭证
    private UmsSdkCredential credential;
}
