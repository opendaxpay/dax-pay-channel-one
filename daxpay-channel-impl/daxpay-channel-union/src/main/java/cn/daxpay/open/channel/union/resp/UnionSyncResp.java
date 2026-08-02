package cn.daxpay.open.channel.union.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 云闪付通道支付同步响应
///
/// 子应用将银联 queryTrans.do 返回的原始状态码(respCode)统一映射为平台标准 tradeStatus,
/// 主应用无需关心通道差异。
///
/// 统一状态码:
/// - SUCCESS 支付成功(respCode=00)
/// - PROGRESS 支付进行中(respCode=03 接受/01 待查)
/// - CLOSED 已关闭(respCode=05 撤销)
@Data
@Accessors(chain = true)
public class UnionSyncResp {

    /// 商户订单号(回显)
    private String outTradeNo;

    /// 统一交易状态(SUCCESS / PROGRESS / CLOSED)
    private String tradeStatus;

    /// 订单金额(单位: 分)
    private Long totalAmount;

    /// 实付金额(单位: 分, 银联 settleAmt)
    private Long realAmount;

    /// 支付成功时间(yyyyMMddHHmmss, 东八区)
    private String payTime;

    /// 银联交易查询凭证(退款时作为 origQryId 必填, 主应用需保存)
    private String queryId;

    /// 买家标识(银联 payerUid / accNo)
    private String buyerId;

    /// 查询失败时的错误信息
    private String errorMsg;
}
