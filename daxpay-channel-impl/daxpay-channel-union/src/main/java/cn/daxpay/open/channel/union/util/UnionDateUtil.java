package cn.daxpay.open.channel.union.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/// # 云闪付通道日期工具
///
/// 银联 ACP 属国内通道, 接口时间字段(txnTime / 支付完成时间)一律按东八区(GMT+8)紧凑字面量传输,
/// 格式为 yyyyMMddHHmmss, 与银联商务(yyyy-MM-dd HH:mm:ss)不同。
///
/// 显式钉死 [ZoneOffset.ofHours](+08:00), 不依赖运行环境时区。
public final class UnionDateUtil {

    /// 银联请求/响应时间格式(yyyyMMddHHmmss)
    private static final DateTimeFormatter TXN_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /// 东八区偏移(北京时间)
    private static final ZoneOffset CST = ZoneOffset.ofHours(8);

    private UnionDateUtil() {
    }

    /// 当前东八区紧凑时间(yyyyMMddHHmmss), 用于银联 txnTime
    public static String txnTime() {
        return OffsetDateTime.now(CST).format(TXN_TIME_FORMATTER);
    }

    /// 解析银联返回的东八区时间字符串(yyyyMMddHHmmss)为 [OffsetDateTime]
    ///
    /// 银联返回无时区时间字面量, 先用 [LocalDateTime] 接住再附加东八区偏移。
    /// (通道时间解析中间步骤, 最终落入实体的仍是 OffsetDateTime)
    public static OffsetDateTime parseCst(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(text, TXN_TIME_FORMATTER).atOffset(CST);
    }
}
