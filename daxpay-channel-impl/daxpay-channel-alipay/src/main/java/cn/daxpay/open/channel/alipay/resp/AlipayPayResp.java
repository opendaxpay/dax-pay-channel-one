package cn.daxpay.open.channel.alipay.resp;

import cn.daxpay.open.channel.alipay.enums.AlipayPayBodyType;
import lombok.Data;

import java.time.OffsetDateTime;

/// # 支付宝通道支付响应
///
/// 子应用调用支付宝 SDK 后回传给主应用, 字段名与支付宝 API 响应字段(camelCase)对齐。
/// `complete=false` 表示需等待异步通知确认最终状态。
@Data
public class AlipayPayResp {

    // ===== 基础交易信息(所有支付方式) =====

    /// 商户订单号(透传 AlipayPayReq.outTradeNo)
    private String outTradeNo;

    /// 支付宝交易号(trade_no)
    private String tradeNo;

    /// 支付内容(HTML 表单 / 二维码链接 / APP 订单串)
    private String payBody;

    /// 支付内容类型
    private AlipayPayBodyType payBodyType;

    // ===== 状态信息 =====

    /// 是否已终态完成(true 表示同步即完成, 无需等待回调; BARCODE 付款码 code=10000 时为 true)
    private Boolean complete;

    // ===== 时间信息(BARCODE 同步成功时返回) =====

    /// 完成时间(gmt_payment)
    private OffsetDateTime finishTime;

    // ===== 金额信息(BARCODE 同步成功时返回, 单位: 分) =====

    /// 订单总金额(total_amount)
    private Long totalAmount;

    /// 买家实付金额(buyer_pay_amount)
    private Long buyerPayAmount;

    /// 商家实收金额(receipt_amount)
    private Long receiptAmount;

    // ===== 用户信息(BARCODE 同步成功时返回) =====

    /// 买家支付宝用户号(buyer_user_id, 2088开头, 持久不变)
    private String buyerUserId;

    /// 买家支付宝开放ID(buyer_open_id)
    private String buyerOpenId;
}
