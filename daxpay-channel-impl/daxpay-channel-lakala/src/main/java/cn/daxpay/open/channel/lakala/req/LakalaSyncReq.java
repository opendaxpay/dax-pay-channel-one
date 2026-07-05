package cn.daxpay.open.channel.lakala.req;

import cn.daxpay.open.channel.lakala.config.LakalaSdkCredential;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/// # 拉卡拉通道订单查询请求
///
/// 走 `/v3/labs/query/tradequery`。
@Data
public class LakalaSyncReq {

    /// 通道调用凭证
    @NotNull(message = "{validation.field.credential.notNull}")
    private LakalaSdkCredential credential;

    /// 商户订单号(与 tradeNo 二选一)
    private String outTradeNo;

    /// 拉卡拉交易号(与 outTradeNo 二选一)
    private String tradeNo;
}
