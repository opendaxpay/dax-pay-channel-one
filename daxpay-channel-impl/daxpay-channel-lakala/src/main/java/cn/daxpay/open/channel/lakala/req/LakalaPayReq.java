package cn.daxpay.open.channel.lakala.req;

import cn.daxpay.open.channel.lakala.config.LakalaSdkCredential;
import cn.daxpay.open.channel.lakala.enums.LakalaPayBodyType;
import cn.daxpay.open.channel.lakala.enums.LakalaPayMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.OffsetDateTime;

/// # 拉卡拉通道支付请求
///
/// 由主应用 dax-pay-open 经声明式 HTTP 客户端转发。
/// 支付方式由 method + accountType + transType 三要素决定:
/// - MICROPAY(条码): 走 `/v3/labs/trans/micropay`, accountType 不传(拉卡拉据 authCode 自动识别)
/// - PREORDER(预下单): 走 `/v3/labs/trans/preorder`, 需显式传 accountType + transType
@Data
public class LakalaPayReq {

    /// 通道调用凭证
    @NotNull(message = "{validation.field.credential.notNull}")
    private LakalaSdkCredential credential;

    /// 商户订单号(主应用支付交易号, 作为拉卡拉 out_trade_no, 异步回调凭此反查)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String outTradeNo;

    /// 订单金额(单位: 分)
    @NotNull(message = "{validation.field.amount.notNull}")
    @Positive(message = "{validation.field.amount.positive}")
    private Long amount;

    /// 商品标题
    @NotBlank(message = "{validation.field.title.notBlank}")
    private String title;

    /// 商品描述
    private String description;

    /// 支付方式(MICROPAY 条码 / PREORDER 预下单)
    @NotNull(message = "{validation.field.method.notNull}")
    private LakalaPayMethod method;

    /// 账户类型(PREORDER 必填: WECHAT / ALIPAY / UQRCODEPAY; MICROPAY 不传)
    private String accountType;

    /// 交易类型(PREORDER 必填: 41扫码 / 51 JSAPI / 61 APP / 71 小程序; MICROPAY 不传)
    private String transType;

    /// 用户标识(JSAPI/MINI 必填: 微信 openid / 支付宝 buyerId)
    private String openId;

    /// 付款码(MICROPAY 必填)
    private String authCode;

    /// 客户端IP
    private String clientIp;

    /// 异步通知地址(由子应用透传给拉卡拉)
    private String notifyUrl;

    /// 订单过期时间(拉卡拉格式 yyyyMMddHHmmss, 子应用调用时格式化; 为空不传)
    private OffsetDateTime expireTime;

    /// 支付内容类型(主应用预先计算好, 子应用透传到响应)
    private LakalaPayBodyType payBodyType;
}
