package cn.daxpay.open.channel.douyin.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/// # 抖音时间解析工具
///
/// 抖音返回的时间字符串存在两种格式(RFC3339 带偏移 / yyyy-MM-dd HH:mm:ss 东八区字面量),
/// 统一解析为 [OffsetDateTime] 供跨 HTTP 下发主应用。
/// 禁止对无时区字面量直接 [OffsetDateTime#parse](强制要求偏移量, 必抛异常)。
@Slf4j
public final class DouyinDateUtil {

    private DouyinDateUtil() {
    }

    /// 解析抖音时间字符串, 兼容 RFC3339 与东八区字面量, 解析失败返回 null(不中断业务)
    public static OffsetDateTime parse(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }
        // 先按 RFC3339(带偏移)直接解析
        try {
            return OffsetDateTime.parse(dateStr);
        } catch (DateTimeParseException ignored) {
            // 无时区字面量走东八区兜底
        }
        try {
            return OffsetDateTime.parse(dateStr.replace(" ", "T") + "+08:00");
        } catch (DateTimeParseException e) {
            log.warn("抖音时间解析失败: {}", dateStr);
            return null;
        }
    }
}
