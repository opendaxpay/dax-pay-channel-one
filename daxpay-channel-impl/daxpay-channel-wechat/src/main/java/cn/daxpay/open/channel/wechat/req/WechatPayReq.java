package cn.daxpay.open.channel.wechat.req;

import cn.daxpay.open.channel.wechat.config.WechatSdkCredential;
import cn.daxpay.open.channel.wechat.enums.WechatPayMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.OffsetDateTime;

/// # 微信通道支付请求
///
/// 专口专用: 由主应用 dax-pay-open 经声明式 HTTP 客户端转发, 字段贴近微信支付 V3 参数语义。
/// 通道调用凭证以强类型 [WechatSdkCredential] 传输; 异步通知地址(notifyUrl)独立承载, 不混入凭证对象。
@Data
public class WechatPayReq {

    /// 商户订单号(主应用支付交易号, 作为微信 out_trade_no, 异步回调凭此反查)
    @NotBlank(message = "{validation.field.outTradeNo.notBlank}")
    private String outTradeNo;

    /// 订单金额(单位: 分, 微信全程使用分)
    @NotNull(message = "{validation.field.amount.notNull}")
    @Positive(message = "{validation.field.amount.positive}")
    private Long amount;

    /// 商品描述(对应微信 description)
    @NotBlank(message = "{validation.field.description.notBlank}")
    private String description;

    /// 支付方式
    @NotNull(message = "{validation.field.method.notNull}")
    private WechatPayMethod method;

    /// 订单过期时间(微信要求 RFC3339 格式, 子应用调用 SDK 时格式化)
    private OffsetDateTime expireTime;

    /// 异步通知地址(由子应用透传给微信)
    private String notifyUrl;

    /// 附加数据(对应微信 attach, 在通知与查询中原样返回)
    private String attach;

    /// 用户标识(JSAPI / MINI 必填; 公众号 openid 或小程序 openid)
    private String openId;

    /// 付款码(MICROPAY 必填, 用户出示的被扫码)
    private String authCode;

    /// 用户终端IP(H5 场景必填, scene_info.payer_client_ip)
    private String payerClientIp;

    /// H5 场景 wap_url(H5 必填)
    private String wapUrl;

    /// H5 场景 wap_name(H5 必填)
    private String wapName;

    /// 是否分账订单(透传微信 profit_sharing=true)
    private Boolean allocation;

    /// 通道调用凭证
    private WechatSdkCredential credential;
}
