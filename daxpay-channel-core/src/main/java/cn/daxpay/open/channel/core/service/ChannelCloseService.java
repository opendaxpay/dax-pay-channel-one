package cn.daxpay.open.channel.core.service;

import cn.daxpay.open.channel.common.dto.close.ChannelCloseReq;
import cn.daxpay.open.channel.common.dto.close.ChannelCloseResp;

public interface ChannelCloseService {
    ChannelCloseResp close(ChannelCloseReq req);
}
