package cn.daxpay.open.channel.core.service;

import lombok.RequiredArgsConstructor;
import cn.daxpay.open.channel.common.exception.ChannelErrorCode;
import cn.daxpay.open.channel.core.exception.ChannelServiceException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChannelRouterService {

    private final Map<String, ChannelPayService> payServiceMap;
    private final Map<String, ChannelRefundService> refundServiceMap;
    private final Map<String, ChannelCloseService> closeServiceMap;
    private final Map<String, ChannelSyncService> syncServiceMap;
    private final Map<String, ChannelCallbackVerifyService> callbackVerifyServiceMap;

    public ChannelPayService getPayService(String channel) {
        ChannelPayService service = payServiceMap.get(channel);
        if (service == null) {
            throw new ChannelServiceException(ChannelErrorCode.CHANNEL_NOT_FOUND, "pay:" + channel);
        }
        return service;
    }

    public ChannelRefundService getRefundService(String channel) {
        ChannelRefundService service = refundServiceMap.get(channel);
        if (service == null) {
            throw new ChannelServiceException(ChannelErrorCode.CHANNEL_NOT_FOUND, "refund:" + channel);
        }
        return service;
    }

    public ChannelCloseService getCloseService(String channel) {
        ChannelCloseService service = closeServiceMap.get(channel);
        if (service == null) {
            throw new ChannelServiceException(ChannelErrorCode.CHANNEL_NOT_FOUND, "close:" + channel);
        }
        return service;
    }

    public ChannelSyncService getSyncService(String channel) {
        ChannelSyncService service = syncServiceMap.get(channel);
        if (service == null) {
            throw new ChannelServiceException(ChannelErrorCode.CHANNEL_NOT_FOUND, "sync:" + channel);
        }
        return service;
    }

    public ChannelCallbackVerifyService getCallbackVerifyService(String channel) {
        ChannelCallbackVerifyService service = callbackVerifyServiceMap.get(channel);
        if (service == null) {
            throw new ChannelServiceException(ChannelErrorCode.CHANNEL_NOT_FOUND, "callback:" + channel);
        }
        return service;
    }
}
