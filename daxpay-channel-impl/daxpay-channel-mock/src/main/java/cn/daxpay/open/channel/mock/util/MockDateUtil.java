package cn.daxpay.open.channel.mock.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/// # Mock 通道时间工具
///
/// 网关报文时间一律 RFC3339 带东八区偏移(如 ``2026-10-26T23:38:00+08:00``, 契约 §七),
/// one 内部统一使用 [OffsetDateTime]; 内部契约时间串(yyyy-MM-dd HH:mm:ss)无时区,
/// 解析时先用 [LocalDateTime] 接住再附加东八区偏移(通道时间解析中间步骤, 最终产物仍是 OffsetDateTime)。
public final class MockDateUtil {

    /// 东八区偏移(业务时区基线)
    private static final ZoneOffset CST = ZoneOffset.ofHours(8);

    /// RFC3339 格式(固定东八区偏移输出)
    private static final DateTimeFormatter RFC3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /// 内部契约时间格式(yyyy-MM-dd HH:mm:ss)
    private static final DateTimeFormatter INTERNAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private MockDateUtil() {
    }

    /// OffsetDateTime → RFC3339(+08:00)
    public static String toRfc3339(OffsetDateTime time) {
        if (time == null) {
            return null;
        }
        return time.withOffsetSameInstant(CST).format(RFC3339);
    }

    /// 内部时间字符串(yyyy-MM-dd HH:mm:ss)→ RFC3339(+08:00)
    ///
    /// 空白返回 null; 非预期格式原样透传, 由网关侧参数校验兜底(网关亦容忍 RFC3339 直传)。
    public static String internalToRfc3339(String internalTime) {
        if (internalTime == null || internalTime.isBlank()) {
            return null;
        }
        try {
            return toRfc3339(LocalDateTime.parse(internalTime, INTERNAL).atOffset(CST));
        } catch (Exception e) {
            return internalTime;
        }
    }

    /// RFC3339 → OffsetDateTime(保留报文自身偏移)
    public static OffsetDateTime fromRfc3339(String rfc3339) {
        if (rfc3339 == null || rfc3339.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(rfc3339);
    }
}
