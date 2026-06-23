package cn.daxpay.open.platform.core.service;

import cn.daxpay.open.platform.core.dto.callback.ChannelCallbackVerifyReq;
import cn.daxpay.open.platform.core.dto.callback.ChannelCallbackVerifyResp;

public interface ChannelCallbackVerifyService {
    ChannelCallbackVerifyResp verify(ChannelCallbackVerifyReq req);
}
