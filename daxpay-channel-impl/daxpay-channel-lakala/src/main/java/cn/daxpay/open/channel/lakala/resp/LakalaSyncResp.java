package cn.daxpay.open.channel.lakala.resp;

import lombok.Data;

import java.time.OffsetDateTime;

/// # 拉卡拉通道订单查询响应
///
/// tradeState 取值: SUCCESS(成功) / FAIL(失败) / CLOSED(已关闭) / 其他(处理中)。
@Data
public class LakalaSyncResp {

    /// 商户订单号
    private String outTradeNo;

    /// 拉卡拉交易号
    private String tradeNo;

    /// 交易状态(trade_state: SUCCESS / FAIL / CLOSED / 其他)
    private String tradeState;

    /// 订单总金额(单位: 分)
    private Long totalAmount;

    /// 买家标识
    private String buyerId;

    /// 完成时间
    private OffsetDateTime finishTime;

    /// 原始响应数据(用于对账/排障)
    private String syncData;
}
