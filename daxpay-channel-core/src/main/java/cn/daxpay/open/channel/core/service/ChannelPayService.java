package cn.daxpay.open.channel.core.service;

import cn.daxpay.open.channel.common.dto.pay.ChannelPayReq;
import cn.daxpay.open.channel.common.dto.pay.ChannelPayResp;

public interface ChannelPayService {
    ChannelPayResp pay(ChannelPayReq req);
}
