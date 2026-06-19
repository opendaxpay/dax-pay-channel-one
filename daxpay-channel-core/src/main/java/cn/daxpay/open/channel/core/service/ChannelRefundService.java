package cn.daxpay.open.channel.core.service;

import cn.daxpay.open.channel.common.dto.refund.ChannelRefundReq;
import cn.daxpay.open.channel.common.dto.refund.ChannelRefundResp;

public interface ChannelRefundService {
    ChannelRefundResp refund(ChannelRefundReq req);
}
