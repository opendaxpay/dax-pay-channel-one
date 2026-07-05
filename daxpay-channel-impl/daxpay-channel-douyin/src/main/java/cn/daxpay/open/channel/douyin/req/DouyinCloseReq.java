package cn.daxpay.open.channel.douyin.req;

import cn.daxpay.open.channel.douyin.config.DouyinSdkCredential;
import lombok.Data;

/// # 抖音通道关闭订单请求
///
/// 关单接口不区分支付方式, 统一使用 NATIVE Service 调用。
@Data
public class DouyinCloseReq {

    /// 商户订单号(主应用支付交易号, 作为抖音 out_trade_no)
    private String outTradeNo;

    /// 通道调用凭证
    private DouyinSdkCredential credential;
}
