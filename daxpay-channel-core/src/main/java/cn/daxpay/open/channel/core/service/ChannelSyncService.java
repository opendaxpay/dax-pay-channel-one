package cn.daxpay.open.channel.core.service;

import cn.daxpay.open.channel.common.dto.sync.ChannelSyncReq;
import cn.daxpay.open.channel.common.dto.sync.ChannelSyncResp;

public interface ChannelSyncService {
    ChannelSyncResp sync(ChannelSyncReq req);
}
