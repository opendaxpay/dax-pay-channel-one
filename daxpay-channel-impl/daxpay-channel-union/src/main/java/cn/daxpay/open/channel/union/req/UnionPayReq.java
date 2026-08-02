package cn.daxpay.open.channel.union.req;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.enums.UnionPayMethod;
import lombok.Data;

/// # 云闪付通道支付请求
///
/// 专口专用: 由主应用 dax-pay-open 经声明式 HTTP 客户端转发。
/// 通过 [#method] 区分主扫/被扫/H5 支付方式, 子应用据此路由到对应银联 ACP 接口。
@Data
public class UnionPayReq {

    /// 商户订单号(主应用支付交易号, 作为银联 orderId)
    private String outTradeNo;

    /// 订单金额(单位: 分, 银联 txnAmt)
    private Long amount;

    /// 商品描述(银联 orderDesc)
    private String description;

    /// 支付方式
    private UnionPayMethod method;

    /// 异步通知地址(由子应用透传给银联作为 backUrl)
    private String notifyUrl;

    /// 付款码(被扫 BARCODE 必填, 用户云闪付 App 展示的付款码, 银联 qrNo 字段)
    private String authCode;

    /// 客户端 IP
    private String clientIp;

    /// 通道调用凭证
    private UnionSdkCredential credential;
}
