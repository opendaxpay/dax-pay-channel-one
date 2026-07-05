package cn.daxpay.open.channel.lakala.resp;

import cn.daxpay.open.channel.lakala.enums.LakalaPayBodyType;
import lombok.Data;

import java.time.OffsetDateTime;

/// # 拉卡拉通道支付响应
///
/// 子应用调用拉卡拉 API 后回传给主应用。
/// `complete=false` 表示需等待异步通知确认最终状态(扫码/JSAPI/APP/小程序);
/// `complete=true` 表示同步即完成(条码 MICROPAY 扣款成功时)。
@Data
public class LakalaPayResp {

    /// 商户订单号(透传 LakalaPayReq.outTradeNo)
    private String outTradeNo;

    /// 拉卡拉交易号(trade_no)
    private String tradeNo;

    /// 支付内容(二维码链接 / JSAPI调起参数JSON / 跳转链接)
    private String payBody;

    /// 支付内容类型
    private LakalaPayBodyType payBodyType;

    /// 是否已终态完成(条码同步成功时为 true)
    private Boolean complete;

    /// 订单总金额(单位: 分, 条码同步成功时返回)
    private Long totalAmount;

    /// 用户实付金额(单位: 分, 条码同步成功时返回)
    private Long payerAmount;

    /// 完成时间
    private OffsetDateTime finishTime;

    /// 买家标识(条码同步成功时返回: 微信 openId / 支付宝 userId / 银联 userId)
    private String buyerId;
}
