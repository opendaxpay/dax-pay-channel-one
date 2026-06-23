package cn.daxpay.open.platform.core.dto.pay;

import lombok.Data;

/// # 通道支付响应
///
/// 子应用调用三方接口后回传给主应用, `complete=false` 表示需等待异步通知确认最终状态。
@Data
public class ChannelPayResp {

    /// 商户订单号
    private String bizOrderNo;

    /// 通道侧订单号(三方交易号)
    private String outOrderNo;

    /// 通道交易流水号(预留)
    private String transOrderNo;

    /// 支付内容(支付链接 / 二维码 / 表单等)
    private String payBody;

    /// 支付内容类型(qr_code / form / order_id)
    private String payBodyType;

    /// 是否已终态完成(true 表示同步即完成, 无需等待回调)
    private Boolean complete;

    /// 完成时间
    private String finishTime;
}
