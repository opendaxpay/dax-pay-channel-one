package org.dromara.daxpay.channel.core.service;

import org.dromara.daxpay.channel.common.dto.pay.ChannelPayReq;
import org.dromara.daxpay.channel.common.dto.pay.ChannelPayResp;

public interface ChannelPayService {
    ChannelPayResp pay(ChannelPayReq req);
}
