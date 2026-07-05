package cn.daxpay.open.channel.ums.req;

import cn.daxpay.open.channel.ums.config.UmsSdkCredential;
import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import lombok.Data;

/// # 银联商务通道支付同步请求
///
/// 扫码查单(queryQrOrder)与 H5 查单(queryH5Order)接口不同,
/// 通过 [method] 区分。
@Data
public class UmsSyncReq {

    /// 商户订单号(扫码查询作为 billNo, H5 查询作为 merOrderId)
    private String outTradeNo;

    /// 订单日期(yyyy-MM-dd, 扫码查询必填)
    private String billDate;

    /// 支付方式(区分扫码查询 / H5 查询)
    private UmsPayMethod method;

    /// 通道调用凭证
    private UmsSdkCredential credential;
}
