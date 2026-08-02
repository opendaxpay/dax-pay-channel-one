package cn.daxpay.open.channel.union.req;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.enums.UnionPayMethod;
import lombok.Data;

/// # 云闪付通道关闭订单请求
///
/// 银联 ACP 关单(交易类型 31)需要原交易查询凭证 queryId(支付成功时银联返回)。
@Data
public class UnionCloseReq {

    /// 商户订单号
    private String outTradeNo;

    /// 原交易查询凭证(银联 queryId, 支付成功/同步时获得)
    private String queryId;

    /// 支付方式
    private UnionPayMethod method;

    /// 通道调用凭证
    private UnionSdkCredential credential;
}
