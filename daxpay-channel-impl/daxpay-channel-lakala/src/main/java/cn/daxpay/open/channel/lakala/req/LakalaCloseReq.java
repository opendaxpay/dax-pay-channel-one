package cn.daxpay.open.channel.lakala.req;

import cn.daxpay.open.channel.lakala.config.LakalaSdkCredential;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/// # 拉卡拉通道关单请求
///
/// 走 `/v3/labs/relation/close`。关单仅对未完成的扫码支付(主扫)有效; 被扫(条码)走撤销。
@Data
public class LakalaCloseReq {

    /// 通道调用凭证
    @NotNull(message = "{validation.field.credential.notNull}")
    private LakalaSdkCredential credential;

    /// 原商户订单号(与 originTradeNo 二选一)
    private String originOutTradeNo;

    /// 原拉卡拉交易号(与 originOutTradeNo 二选一)
    private String originTradeNo;

    /// 客户端IP
    private String clientIp;
}
