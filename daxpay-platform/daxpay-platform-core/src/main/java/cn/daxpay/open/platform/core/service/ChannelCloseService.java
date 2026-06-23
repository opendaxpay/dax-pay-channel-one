package cn.daxpay.open.platform.core.service;

import cn.daxpay.open.platform.core.dto.close.ChannelCloseReq;
import cn.daxpay.open.platform.core.dto.close.ChannelCloseResp;

public interface ChannelCloseService {
    ChannelCloseResp close(ChannelCloseReq req);
}
