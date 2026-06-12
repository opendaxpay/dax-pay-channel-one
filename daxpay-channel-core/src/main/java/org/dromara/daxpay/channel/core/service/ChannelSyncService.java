package org.dromara.daxpay.channel.core.service;

import org.dromara.daxpay.channel.common.dto.sync.ChannelSyncReq;
import org.dromara.daxpay.channel.common.dto.sync.ChannelSyncResp;

public interface ChannelSyncService {
    ChannelSyncResp sync(ChannelSyncReq req);
}
