package cn.daxpay.open.channel.douyin.req;

import cn.daxpay.open.channel.douyin.config.DouyinSdkCredential;
import lombok.Data;

/// # 抖音通道退款同步请求
///
/// 通过退款单号查询退款最终状态。
@Data
public class DouyinRefundSyncReq {

    /// 退款单号(主应用退款单号, 作为抖音 out_refund_no)
    private String outRefundNo;

    /// 通道调用凭证
    private DouyinSdkCredential credential;
}
