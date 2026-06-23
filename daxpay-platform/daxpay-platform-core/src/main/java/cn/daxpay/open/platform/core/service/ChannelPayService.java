package cn.daxpay.open.platform.core.service;

import cn.daxpay.open.platform.core.dto.pay.ChannelPayReq;
import cn.daxpay.open.platform.core.dto.pay.ChannelPayResp;

public interface ChannelPayService {
    ChannelPayResp pay(ChannelPayReq req);
}
