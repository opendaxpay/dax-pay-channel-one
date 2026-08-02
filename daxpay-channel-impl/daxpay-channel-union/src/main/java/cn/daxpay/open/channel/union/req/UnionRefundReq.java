package cn.daxpay.open.channel.union.req;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.enums.UnionPayMethod;
import lombok.Data;

/// # 云闪付通道退款请求
///
/// 银联 ACP 退货(交易类型 04)必须传入原交易查询凭证 origQueryId,
/// 该值在支付成功或同步查询时由银联返回(银联字段 queryId)。
@Data
public class UnionRefundReq {

    /// 原商户订单号
    private String outTradeNo;

    /// 原交易查询凭证(银联 origQryId, 必填)
    private String origQueryId;

    /// 退款单号(主应用退款单号, 作为银联退款 orderId)
    private String outRefundNo;

    /// 退款金额(单位: 分, 银联 txnAmt)
    private Long refundAmount;

    /// 退款异步通知地址(银联 backUrl)
    private String notifyUrl;

    /// 支付方式
    private UnionPayMethod method;

    /// 通道调用凭证
    private UnionSdkCredential credential;
}
