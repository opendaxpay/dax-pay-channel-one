package cn.daxpay.open.channel.core.service;

import cn.daxpay.open.channel.common.dto.callback.ChannelCallbackVerifyReq;
import cn.daxpay.open.channel.common.dto.callback.ChannelCallbackVerifyResp;

public interface ChannelCallbackVerifyService {
    ChannelCallbackVerifyResp verify(ChannelCallbackVerifyReq req);
}
