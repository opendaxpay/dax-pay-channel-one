package cn.daxpay.open.channel.core.cache.entity;

import lombok.Data;

import java.time.OffsetDateTime;

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
    private OffsetDateTime expireTime;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
