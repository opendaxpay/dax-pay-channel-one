package cn.daxpay.open.channel.ums.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 银联商务通道退款同步响应
///
/// 子应用将扫码与 H5 两种不同的原始退款状态码
/// 统一映射为平台标准 refundStatus。
///
/// 统一状态码:
/// - SUCCESS 退款成功
/// - PROGRESS 退款处理中
/// - CLOSED 退款关闭(不可继续)
@Data
@Accessors(chain = true)
public class UmsRefundSyncResp {

    /// 退款单号(回显)
    private String outRefundNo;

    /// 统一退款状态(SUCCESS / PROGRESS / CLOSED)
    private String refundStatus;

    /// 退款金额(单位: 分)
    private Long refundAmount;

    /// 退款完成时间(yyyy-MM-dd HH:mm:ss)
    private String finishTime;

    /// 查询失败时的错误信息
    private String errorMsg;
}
