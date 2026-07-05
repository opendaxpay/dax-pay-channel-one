package cn.daxpay.open.channel.lakala.req;

import cn.daxpay.open.channel.lakala.config.LakalaSdkCredential;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/// # 拉卡拉通道退款查询请求
@Data
public class LakalaRefundSyncReq {

    /// 通道调用凭证
    @NotNull(message = "{validation.field.credential.notNull}")
    private LakalaSdkCredential credential;

    /// 商户退款单号(与 originTradeNo 二选一)
    private String outRefundNo;

    /// 原拉卡拉退款交易号(与 outRefundNo 二选一)
    private String originTradeNo;
}
