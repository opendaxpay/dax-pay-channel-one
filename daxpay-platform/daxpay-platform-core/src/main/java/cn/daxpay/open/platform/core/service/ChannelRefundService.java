package cn.daxpay.open.platform.core.service;

import cn.daxpay.open.platform.core.dto.refund.ChannelRefundReq;
import cn.daxpay.open.platform.core.dto.refund.ChannelRefundResp;

public interface ChannelRefundService {
    ChannelRefundResp refund(ChannelRefundReq req);
}
