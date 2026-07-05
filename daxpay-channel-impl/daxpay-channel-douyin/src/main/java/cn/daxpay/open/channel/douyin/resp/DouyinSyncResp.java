package cn.daxpay.open.channel.douyin.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 抖音通道支付同步响应
///
/// 抖音交易状态码:
/// - SUCCESS 支付成功
/// - REFUND 转入退款
/// - NOTPAY 未支付
/// - USERPAYING 用户支付中
/// - CLOSED 已关闭
/// - PAYERROR 支付失败
@Data
@Accessors(chain = true)
public class DouyinSyncResp {

    /// 商户订单号(回显)
    private String outTradeNo;

    /// 抖音交易号(transactionId)
    private String transactionId;

    /// 交易状态(SUCCESS / REFUND / NOTPAY / USERPAYING / CLOSED / PAYERROR)
    private String tradeState;

    /// 订单金额(单位: 分)
    private Long totalAmount;

    /// 买家 openid
    private String openid;

    /// 支付成功时间(RFC3339)
    private String successTime;

    /// 查询失败时的错误码(如 ORDER_NOT_EXIST)
    private String errorCode;

    /// 查询失败时的错误信息
    private String errorMsg;
}
