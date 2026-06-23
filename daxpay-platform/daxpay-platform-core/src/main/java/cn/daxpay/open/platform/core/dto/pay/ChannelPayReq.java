package cn.daxpay.open.platform.core.dto.pay;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

/// # 通道支付请求
///
/// 由主应用 dax-pay-open 通过声明式客户端转发而来, 子应用按 `channel` 路由到对应实现。
@Data
public class ChannelPayReq {

    /// 通道编码(如 alipay / wechat)
    @NotBlank(message = "{validation.field.channel.notBlank}")
    private String channel;

    /// 商户订单号
    @NotBlank(message = "{validation.field.bizOrderNo.notBlank}")
    private String bizOrderNo;

    /// 订单金额(单位: 分)
    @NotNull(message = "{validation.field.amount.notNull}")
    private Long amount;

    /// 订单标题
    @NotBlank(message = "{validation.field.subject.notBlank}")
    private String subject;

    /// 订单描述
    private String description;

    /// 支付方式(如 alipay_wap / alipay_qr / wechat_h5)
    @NotBlank(message = "{validation.field.method.notBlank}")
    private String method;

    /// 订单过期时间
    private String expireTime;

    /// 其他支付方式(预留扩展)
    private String otherMethod;

    /// 通道配置参数(为空时进入 Demo 模式, 返回模拟响应)
    @NotNull(message = "{validation.field.config.notNull}")
    private Map<String, Object> config;
}
