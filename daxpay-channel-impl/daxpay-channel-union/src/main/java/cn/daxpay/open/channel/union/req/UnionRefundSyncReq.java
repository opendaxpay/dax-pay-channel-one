package cn.daxpay.open.channel.union.req;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.enums.UnionPayMethod;
import lombok.Data;

/// # 云闪付通道退款同步请求
///
/// 通过银联 queryTrans.do 接口查询退款单最终状态。
@Data
public class UnionRefundSyncReq {

    /// 退款单号(主应用退款单号, 银联退款 orderId)
    private String outRefundNo;

    /// 支付方式
    private UnionPayMethod method;

    /// 通道调用凭证
    private UnionSdkCredential credential;
}
