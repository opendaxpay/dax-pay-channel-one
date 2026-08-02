package cn.daxpay.open.channel.union.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 云闪付通道退款响应
///
/// 银联 ACP 退货应答 respCode:
/// - 00 成功 / 03 处理中 / 01 失败
/// 子应用统一映射为 SUCCESS / PROCESSING / FAIL。
@Data
@Accessors(chain = true)
public class UnionRefundResp {

    /// 退款单号(回显)
    private String outRefundNo;

    /// 退款状态(SUCCESS / PROCESSING / FAIL)
    private String refundStatus;

    /// 退款完成时间(yyyyMMddHHmmss, 东八区)
    private String finishTime;
}
