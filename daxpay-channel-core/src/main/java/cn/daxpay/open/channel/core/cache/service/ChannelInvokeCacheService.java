package cn.daxpay.open.channel.core.cache.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.core.cache.dao.ChannelInvokeCacheManager;
import cn.daxpay.open.channel.core.cache.entity.ChannelInvokeCache;
import cn.daxpay.open.channel.core.enums.CacheStatusEnum;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelInvokeCacheService {

    private final ChannelInvokeCacheManager channelInvokeCacheManager;

    public boolean tryAcquire(String channel, String action, String bizNo, String requestHash) {
        String cacheKey = buildCacheKey(channel, action, bizNo);
        try {
            ChannelInvokeCache cache = new ChannelInvokeCache();
            cache.setId(IdUtil.getSnowflakeNextIdStr());
            cache.setCacheKey(cacheKey);
            cache.setChannel(channel);
            cache.setAction(action);
            cache.setBizNo(bizNo);
            cache.setRequestHash(requestHash);
            cache.setStatus(CacheStatusEnum.PROCESSING.name());
            cache.setExpireTime(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5));
            cache.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
            cache.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
            channelInvokeCacheManager.save(cache);
            return true;
        } catch (DuplicateKeyException e) {
            log.warn("缓存键冲突，请求正在处理中: {}", cacheKey);
            return false;
        }
    }

    public void setSuccess(String channel, String action, String bizNo, String responseBody) {
        String cacheKey = buildCacheKey(channel, action, bizNo);
        channelInvokeCacheManager.findByCacheKey(cacheKey).ifPresent(cache -> {
            cache.setStatus(CacheStatusEnum.SUCCESS.name());
            cache.setResponseBody(responseBody);
            cache.setExpireTime(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24));
            cache.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
            channelInvokeCacheManager.updateById(cache);
        });
    }

    public void setFail(String channel, String action, String bizNo, String errorMsg) {
        String cacheKey = buildCacheKey(channel, action, bizNo);
        channelInvokeCacheManager.findByCacheKey(cacheKey).ifPresent(cache -> {
            cache.setStatus(CacheStatusEnum.FAIL.name());
            cache.setErrorMsg(errorMsg);
            cache.setExpireTime(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));
            cache.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
            channelInvokeCacheManager.updateById(cache);
        });
    }

    public Optional<String> getCachedResponse(String channel, String action, String bizNo) {
        String cacheKey = buildCacheKey(channel, action, bizNo);
        return channelInvokeCacheManager.findByCacheKey(cacheKey)
                .filter(c -> CacheStatusEnum.SUCCESS.name().equals(c.getStatus()))
                .map(ChannelInvokeCache::getResponseBody);
    }

    public Optional<String> getConcurrentStatus(String channel, String action, String bizNo) {
        String cacheKey = buildCacheKey(channel, action, bizNo);
        return channelInvokeCacheManager.findByCacheKey(cacheKey)
                .filter(c -> CacheStatusEnum.PROCESSING.name().equals(c.getStatus()))
                .map(ChannelInvokeCache::getStatus);
    }

    private String buildCacheKey(String channel, String action, String bizNo) {
        return channel + ":" + action + ":" + bizNo;
    }
}
