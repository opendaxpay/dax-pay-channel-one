package cn.daxpay.open.channel.wechat.req;

import cn.daxpay.open.channel.wechat.config.WechatSdkCredential;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/// # 微信通道退款同步请求
///
/// 主应用经声明式 HTTP 客户端下发, 子应用调用微信 V3 `查询单笔退款` 接口查询退款状态。
/// outRefundNo 为退款单号, 必传。
@Data
public class WechatRefundSyncReq {

    /// 退款单号(对应微信 out_refund_no, 必传)
    @NotBlank(message = "{validation.field.outRequestNo.notBlank}")
    private String outRefundNo;

    /// 通道调用凭证
    private WechatSdkCredential credential;
}
