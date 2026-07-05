package cn.daxpay.open.channel.wechat.resp;

import cn.daxpay.open.channel.wechat.enums.WechatPayBodyType;
import lombok.Data;

import java.time.OffsetDateTime;

/// # 微信通道支付响应
///
/// 子应用调用微信 SDK 后回传给主应用。
/// `complete=false` 表示需等待异步通知确认最终状态(JSAPI/NATIVE/APP/H5 均为异步);
/// `complete=true` 表示同步即完成(付款码 MICROPAY V3 codepay 扣款成功时)。
///
/// payBody 内容由 payBodyType 决定:
/// - LINK: H5 跳转链接
/// - QR_CODE: NATIVE 二维码内容
/// - IDENTIFIER: JSAPI/MINI 调起参数 JSON
/// - APP_ORDER_STR: APP 调起参数 JSON
@Data
public class WechatPayResp {

    // ===== 基础交易信息(所有支付方式) =====

    /// 商户订单号(透传 WechatPayReq.outTradeNo)
    private String outTradeNo;

    /// 微信支付订单号(transaction_id, 支付成功后返回; 付款码同步成功时返回)
    private String transactionId;

    /// 支付内容(H5 跳转链接 / 二维码内容 / 调起参数 JSON)
    private String payBody;

    /// 支付内容类型
    private WechatPayBodyType payBodyType;

    // ===== 状态信息 =====

    /// 是否已终态完成(true 表示同步即完成; 付款码 trade_state=SUCCESS 时为 true)
    private Boolean complete;

    // ===== 时间信息(付款码同步成功时返回) =====

    /// 完成时间(success_time)
    private OffsetDateTime finishTime;

    // ===== 金额信息(付款码同步成功时返回, 单位: 分) =====

    /// 订单总金额(total)
    private Long totalAmount;

    /// 用户支付金额(payer_total)
    private Long payerTotal;

    // ===== 用户信息 =====

    /// 用户标识(openid, 可能不返回)
    private String openId;
}
