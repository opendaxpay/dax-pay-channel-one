package cn.daxpay.open.channel.douyin.resp;

import cn.daxpay.open.channel.douyin.enums.DouyinPayBodyType;
import lombok.Data;
import lombok.experimental.Accessors;

/// # 抖音通道支付响应
@Data
@Accessors(chain = true)
public class DouyinPayResp {

    /// 商户订单号(回显)
    private String outTradeNo;

    /// 支付内容(二维码链接 / prepayId / H5 跳转地址)
    private String payBody;

    /// 支付内容类型
    private DouyinPayBodyType payBodyType;
}
