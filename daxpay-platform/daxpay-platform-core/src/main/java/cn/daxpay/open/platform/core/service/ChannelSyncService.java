package cn.daxpay.open.platform.core.service;

import cn.daxpay.open.platform.core.dto.sync.ChannelSyncReq;
import cn.daxpay.open.platform.core.dto.sync.ChannelSyncResp;

public interface ChannelSyncService {
    ChannelSyncResp sync(ChannelSyncReq req);
}
