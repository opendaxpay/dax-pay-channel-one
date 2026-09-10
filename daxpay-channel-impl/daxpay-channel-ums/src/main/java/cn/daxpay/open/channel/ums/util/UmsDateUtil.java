package cn.daxpay.open.channel.ums.util;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/// # 银联商务通道日期工具
///
/// 银联商务属国内通道, 接口时间字段(requestTimestamp / billDate / H5 签名时间戳)
/// 一律按东八区(GMT+8)字面量传输, 与银商服务器时间保持一致。
///
/// 历史问题: 早期使用 `DateUtil.format(new Date())` 依赖 JVM 默认时区,
/// 当容器/主机默认时区为 UTC 时, 输出时间会比北京时间慢 8 小时,
/// 触发银商 `ABNORMAL_REQUEST_TIME`(请求时间跟当前系统时间差异过大)错误。
///
/// 本工具显式钉死 [ZoneOffset.ofHours](+08:00), 与支付宝 / 抖音等国内通道的时区处理姿势一致,
/// 不再依赖运行环境时区。
public final class UmsDateUtil {

    /// 银联商务请求时间戳格式(yyyy-MM-dd HH:mm:ss)
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /// 银联商务账单日期格式(yyyy-MM-dd)
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /// H5 跳转链接签名时间戳格式(yyyyMMddHHmmss)
    private static final DateTimeFormatter H5_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /// 东八区偏移(北京时间)
    private static final ZoneOffset CST = ZoneOffset.ofHours(8);

    private UmsDateUtil() {
    }

    /// 当前东八区时间(yyyy-MM-dd HH:mm:ss), 用于 requestTimestamp
    public static String nowDateTime() {
        return OffsetDateTime.now(CST).format(DATETIME_FORMATTER);
    }

    /// 当前东八区日期(yyyy-MM-dd), 用于 billDate
    public static String todayDate() {
        return OffsetDateTime.now(CST).format(DATE_FORMATTER);
    }

    /// 当前东八区紧凑时间戳(yyyyMMddHHmmss), 用于 H5 跳转链接签名时间戳
    public static String h5Timestamp() {
        return OffsetDateTime.now(CST).format(H5_TIMESTAMP_FORMATTER);
    }

    /// 将 UTC OffsetDateTime 转为东八区日期(yyyy-MM-dd)
    ///
    /// 主应用存储统一为 UTC, 银联商务 billDate 要求北京时间字面量,
    /// 查询/退款/退款同步场景由子应用调用本方法按通道时区转换。
    public static String formatCstDate(OffsetDateTime time) {
        if (Objects.isNull(time)) {
            return null;
        }
        return time.withOffsetSameInstant(CST).format(DATE_FORMATTER);
    }
}
