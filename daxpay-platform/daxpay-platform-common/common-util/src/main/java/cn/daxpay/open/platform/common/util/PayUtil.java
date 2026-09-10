package cn.daxpay.open.platform.common.util;

import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/// # 支付金额工具类
///
/// 统一处理通道适配中的金额单位换算(分 ↔ 元)与精度控制, 对标主项目 dax-pay-open 的 `AmountUtil`。
///
/// - 分 → 元: 保留 2 位小数, 四舍五入(`RoundingMode.HALF_UP`)
/// - 元 → 分: 默认向下取整截断(`RoundingMode.DOWN`), `HalfUp` 后缀变体为四舍五入
///
/// 通道 DTO 中金额字段多为 `Long`(单位: 分), 故额外提供 `conversionFenToYuan(long)` 重载。
///
/// @author xxm
/// @since 2026/7/1
@UtilityClass
public class PayUtil {

    /// 元转分, 保留两位小数, 舍弃后所有尾数
    ///
    /// 例如: 10.25 -> 1025, 10.256 -> 1025
    ///
    /// @param yuan 元
    /// @return 分
    public int conversionYuanToFen(BigDecimal yuan) {
        return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.DOWN).intValue();
    }

    /// 元转分, 四舍五入保留整数
    ///
    /// 例如: 10.25 -> 1025, 10.256 -> 1026
    ///
    /// @param yuan 元, 不能为 null 或空
    /// @return 分
    public int conversionYuanToFenHalfUp(String yuan) {
        if (StrUtil.isBlank(yuan)) {
            return 0;
        }
        return new BigDecimal(yuan).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /// 元转分, 保留两位小数, 舍弃后所有尾数, 元可能为空, 为空时返回 0
    ///
    /// @param yuan 元
    /// @param allowNull 是否允许为空
    /// @return 分
    public int conversionYuanToFen(BigDecimal yuan, boolean allowNull) {
        if (allowNull && Objects.isNull(yuan)) {
            return 0;
        }
        return conversionYuanToFen(yuan);
    }

    /// 元转分, 四舍五入保留整数
    ///
    /// 例如: 10.25 -> 1025, 10.256 -> 1026
    ///
    /// @param yuan 元
    /// @return 分
    public int conversionYuanToFenHalfUp(BigDecimal yuan) {
        return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /// 分转元, 保留两位小数, 四舍五入
    ///
    /// 例如: 1025 -> 10.25, 1026 -> 10.26, 1020 -> 10.20
    ///
    /// @param fen 分
    /// @return 元
    public BigDecimal conversionFenToYuan(int fen) {
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /// 分转元, 保留两位小数, 四舍五入
    ///
    /// 通道 DTO 金额字段通常为 `Long`, 此重载便于直接传入, 无需手动转换。
    ///
    /// 例如: 1025 -> 10.25, 1026 -> 10.26
    ///
    /// @param fen 分
    /// @return 元
    public BigDecimal conversionFenToYuan(long fen) {
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /// 分转元, 保留两位小数, 四舍五入
    ///
    /// 例如: 1025 -> 10.25, 1026 -> 10.26
    ///
    /// @param fen 分
    /// @return 元
    public BigDecimal conversionFenToYuan(BigDecimal fen) {
        return fen.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /// 分转元, 保留两位小数, 四舍五入
    ///
    /// 例如: "1025" -> 10.25, "1026" -> 10.26
    ///
    /// @param fen 分
    /// @return 元
    public BigDecimal conversionFenToYuan(String fen) {
        return new BigDecimal(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
