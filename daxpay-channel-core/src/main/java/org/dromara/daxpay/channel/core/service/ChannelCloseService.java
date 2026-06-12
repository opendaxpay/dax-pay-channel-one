package org.dromara.daxpay.channel.core.service;

import org.dromara.daxpay.channel.common.dto.close.ChannelCloseReq;
import org.dromara.daxpay.channel.common.dto.close.ChannelCloseResp;

public interface ChannelCloseService {
    ChannelCloseResp close(ChannelCloseReq req);
}
