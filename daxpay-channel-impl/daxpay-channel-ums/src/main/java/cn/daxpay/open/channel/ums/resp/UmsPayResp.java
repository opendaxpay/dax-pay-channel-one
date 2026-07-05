package cn.daxpay.open.channel.ums.resp;

import cn.daxpay.open.channel.ums.enums.UmsPayBodyType;
import lombok.Data;
import lombok.experimental.Accessors;

/// # 银联商务通道支付响应
@Data
@Accessors(chain = true)
public class UmsPayResp {

    /// 商户订单号(回显)
    private String outTradeNo;

    /// 支付内容(扫码: billQRCode 二维码链接; H5: 跳转链接)
    private String payBody;

    /// 支付内容类型
    private UmsPayBodyType payBodyType;
}
