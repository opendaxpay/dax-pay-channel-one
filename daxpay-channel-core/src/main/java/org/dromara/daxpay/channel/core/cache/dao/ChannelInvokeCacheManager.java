package org.dromara.daxpay.channel.core.cache.dao;

import lombok.RequiredArgsConstructor;
import org.dromara.daxpay.channel.core.cache.entity.ChannelInvokeCache;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChannelInvokeCacheManager {

    private final JdbcTemplate jdbcTemplate;

    public void save(ChannelInvokeCache cache) {
        jdbcTemplate.update("""
                INSERT INTO channel_invoke_cache (id, cache_key, channel, action, biz_no, request_hash,
                    response_body, status, error_msg, expire_time, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                cache.getId(), cache.getCacheKey(), cache.getChannel(), cache.getAction(),
                cache.getBizNo(), cache.getRequestHash(), cache.getResponseBody(),
                cache.getStatus(), cache.getErrorMsg(), cache.getExpireTime(),
                cache.getCreateTime(), cache.getUpdateTime());
    }

    public Optional<ChannelInvokeCache> findByCacheKey(String cacheKey) {
        var list = jdbcTemplate.query(
                "SELECT * FROM channel_invoke_cache WHERE cache_key = ?",
                new BeanPropertyRowMapper<>(ChannelInvokeCache.class),
                cacheKey);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void updateById(ChannelInvokeCache cache) {
        jdbcTemplate.update("""
                UPDATE channel_invoke_cache SET cache_key=?, channel=?, action=?, biz_no=?,
                    request_hash=?, response_body=?, status=?, error_msg=?, expire_time=?,
                    create_time=?, update_time=?
                WHERE id = ?
                """,
                cache.getCacheKey(), cache.getChannel(), cache.getAction(), cache.getBizNo(),
                cache.getRequestHash(), cache.getResponseBody(), cache.getStatus(),
                cache.getErrorMsg(), cache.getExpireTime(), cache.getCreateTime(),
                cache.getUpdateTime(), cache.getId());
    }

    public void deleteExpired() {
        jdbcTemplate.update("DELETE FROM channel_invoke_cache WHERE expire_time < ?", LocalDateTime.now());
    }
}
