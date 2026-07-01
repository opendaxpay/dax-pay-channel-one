package cn.daxpay.open.channel.alipay.req;

import cn.daxpay.open.channel.alipay.config.AlipaySdkCredential;
import cn.daxpay.open.channel.alipay.enums.AlipayPayMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.OffsetDateTime;

/// # 支付宝通道支付请求
///
/// 专口专用: 由主应用 dax-pay-open 经声明式 HTTP 客户端转发, 字段贴近支付宝开放平台参数语义。
/// 通道调用凭证以强类型 [AlipaySdkCredential] 传输; 异步通知地址(notifyUrl)独立承载, 不混入凭证对象。
@Data
public class AlipayPayReq {

    /// 商户订单号(主应用支付交易号, 作为支付宝 out_trade_no, 异步回调凭此反查)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String outTradeNo;

    /// 订单金额(单位: 分, 子应用调用 SDK 时转为元)
    @NotNull(message = "{validation.field.amount.notNull}")
    @Positive(message = "{validation.field.amount.positive}")
    private Long amount;

    /// 订单标题
    @NotBlank(message = "{validation.field.subject.notBlank}")
    private String subject;

    /// 订单描述(对应支付宝 body 字段)
    private String body;

    /// 支付方式
    @NotNull(message = "{validation.field.method.notNull}")
    private AlipayPayMethod method;

    /// 订单过期时间
    private OffsetDateTime expireTime;

    /// 异步通知地址(由子应用透传给支付宝)
    private String notifyUrl;

    /// 付款码(BARCODE 付款码支付必填, 用户出示的被扫码)
    private String authCode;

    /// 买家标识(JSAPI 小程序支付必填; 2088 开头为支付宝用户ID, 否则视为小程序 openid)
    private String openId;

    /// 通道调用凭证
    private AlipaySdkCredential credential;
}
