package org.dromara.daxpay.channel.core.service;

import org.dromara.daxpay.channel.common.dto.callback.ChannelCallbackVerifyReq;
import org.dromara.daxpay.channel.common.dto.callback.ChannelCallbackVerifyResp;

public interface ChannelCallbackVerifyService {
    ChannelCallbackVerifyResp verify(ChannelCallbackVerifyReq req);
}
