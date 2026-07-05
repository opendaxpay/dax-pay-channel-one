package cn.daxpay.open.channel.ums.req;

import cn.daxpay.open.channel.ums.config.UmsSdkCredential;
import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import lombok.Data;

/// # 银联商务通道支付请求
///
/// 专口专用: 由主应用 dax-pay-open 经声明式 HTTP 客户端转发。
/// 通过 [method] 区分扫码/H5/小程序等支付方式, 子应用据此路由到对应银联商务接口。
@Data
public class UmsPayReq {

    /// 商户订单号(主应用支付交易号, 作为银联 billNo/merOrderId)
    private String outTradeNo;

    /// 订单金额(单位: 分)
    private Long amount;

    /// 商品描述(银联 goods.goodsName)
    private String description;

    /// 支付方式
    private UmsPayMethod method;

    /// 异步通知地址(由子应用透传给银联商务)
    private String notifyUrl;

    /// 客户端 IP(H5 场景信息用)
    private String clientIp;

    /// 是否限制信用卡支付
    private Boolean limitCreditCard;

    /// 微信 AppId(微信小程序收银台支付时必填, 用于 subAppId)
    private String wxAppId;

    /// 通道调用凭证
    private UmsSdkCredential credential;
}
