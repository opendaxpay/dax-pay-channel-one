package cn.daxpay.open.channel.douyin.resp;

import lombok.Data;
import lombok.experimental.Accessors;

/// # 抖音通道关闭订单响应
@Data
@Accessors(chain = true)
public class DouyinCloseResp {

    /// 商户订单号(回显)
    private String outTradeNo;
}
