package cn.daxpay.open.channel.union.req;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.enums.UnionPayMethod;
import lombok.Data;

/// # 云闪付通道支付同步请求
///
/// 通过银联 queryTrans.do 接口查询订单最终状态。
@Data
public class UnionSyncReq {

    /// 商户订单号(银联 orderId)
    private String outTradeNo;

    /// 支付方式
    private UnionPayMethod method;

    /// 通道调用凭证
    private UnionSdkCredential credential;
}
