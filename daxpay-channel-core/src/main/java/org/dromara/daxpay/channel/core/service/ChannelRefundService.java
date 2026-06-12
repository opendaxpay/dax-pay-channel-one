package org.dromara.daxpay.channel.core.service;

import org.dromara.daxpay.channel.common.dto.refund.ChannelRefundReq;
import org.dromara.daxpay.channel.common.dto.refund.ChannelRefundResp;

public interface ChannelRefundService {
    ChannelRefundResp refund(ChannelRefundReq req);
}
