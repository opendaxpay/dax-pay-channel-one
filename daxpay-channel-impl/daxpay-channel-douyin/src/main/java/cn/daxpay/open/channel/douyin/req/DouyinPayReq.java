package cn.daxpay.open.channel.douyin.req;

import cn.daxpay.open.channel.douyin.config.DouyinSdkCredential;
import cn.daxpay.open.channel.douyin.enums.DouyinPayMethod;
import lombok.Data;

import java.time.OffsetDateTime;

/// # 抖音通道支付请求
///
/// 专口专用: 由主应用 dax-pay-open 经声明式 HTTP 客户端转发, 字段贴近抖音开放平台参数语义。
/// 通道调用凭证以强类型 [DouyinSdkCredential] 传输; 异步通知地址(notifyUrl)独立承载, 不混入凭证对象。
@Data
public class DouyinPayReq {

    /// 商户订单号(主应用支付交易号, 作为抖音 out_trade_no, 异步回调凭此反查)
    private String outTradeNo;

    /// 订单金额(单位: 分, 抖音 SDK 直接使用分, 不做转换)
    private Long amount;

    /// 商品描述(最长 127 字符, 抖音 description 字段)
    private String description;

    /// 支付方式
    private DouyinPayMethod method;

    /// 买家标识(JSAPI 支付必填, 抖音 openid)
    private String openId;

    /// 客户端 IP(用于风控场景信息)
    private String clientIp;

    /// 订单过期时间(子应用调用 SDK 时转为 RFC3339 格式)
    private OffsetDateTime expiredTime;

    /// 异步通知地址(由子应用透传给抖音)
    private String notifyUrl;

    /// 是否分账订单(透传抖音分账标识)
    private Boolean allocation;

    /// 通道调用凭证
    private DouyinSdkCredential credential;
}
