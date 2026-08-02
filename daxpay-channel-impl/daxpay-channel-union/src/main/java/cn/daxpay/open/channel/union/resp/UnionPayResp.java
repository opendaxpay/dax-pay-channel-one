package cn.daxpay.open.channel.union.resp;

import cn.daxpay.open.channel.union.enums.UnionPayBodyType;
import lombok.Data;
import lombok.experimental.Accessors;

/// # 云闪付通道支付响应
@Data
@Accessors(chain = true)
public class UnionPayResp {

    /// 商户订单号(回显)
    private String outTradeNo;

    /// 支付内容(主扫: qrNo 二维码内容; 被扫: 空; H5: 跳转链接)
    private String payBody;

    /// 支付内容类型
    private UnionPayBodyType payBodyType;
}
