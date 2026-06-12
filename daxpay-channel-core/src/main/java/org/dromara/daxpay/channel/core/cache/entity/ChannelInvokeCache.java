package org.dromara.daxpay.channel.core.cache.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChannelInvokeCache {
    private String id;
    private String cacheKey;
    private String channel;
    private String action;
    private String bizNo;
    private String requestHash;
    private String responseBody;
    private String status;
    private String errorMsg;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
