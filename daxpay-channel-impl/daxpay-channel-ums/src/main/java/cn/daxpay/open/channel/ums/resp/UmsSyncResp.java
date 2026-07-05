package cn.daxpay.open.channel.ums.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 银联商务通道支付同步响应
///
/// 子应用将扫码(billStatus)与 H5(status)两种不同的原始状态码
/// 统一映射为平台标准 tradeStatus, 主应用无需关心通道差异。
///
/// 统一状态码:
/// - SUCCESS 支付成功(扫码 PAID/REFUND, H5 TRADE_SUCCESS)
/// - PROGRESS 支付进行中(UNPAID)
/// - CLOSED 已关闭(扫码 CLOSED, H5 TRADE_CLOSED)
@Data
@Accessors(chain = true)
public class UmsSyncResp {

    /// 商户订单号(回显)
    private String outTradeNo;

    /// 统一交易状态(SUCCESS / PROGRESS / CLOSED)
    private String tradeStatus;

    /// 订单金额(单位: 分)
    private Long totalAmount;

    /// 实付金额(单位: 分)
    private Long realAmount;

    /// 支付成功时间(yyyy-MM-dd HH:mm:ss)
    private String payTime;

    /// 买家标识(支付宝 buyer_user_id / 微信 openid / 银联 payerUid)
    private String buyerId;

    /// 支付厂商(Alipay / WXPay / UnionPay / ACP / UAC)
    private String targetSys;

    /// 第三方订单号(支付宝/微信/银联的交易号)
    private String targetOrderId;

    /// 查询失败时的错误信息
    private String errorMsg;
}
