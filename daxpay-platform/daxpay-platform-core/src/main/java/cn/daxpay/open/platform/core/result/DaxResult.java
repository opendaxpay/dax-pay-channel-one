package cn.daxpay.open.platform.core.result;

import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import lombok.Data;
import org.slf4j.MDC;

/// # 统一响应封装
///
/// 子应用所有接口统一返回本对象, 与主应用 dax-pay-open 保持一致的响应格式。
/// 主应用声明式客户端只凭响应体中的 `code` 字段判断成败, 故 HTTP 状态码始终为 200。
@Data
public class DaxResult<T> {
    /// 业务状态码(0 表示成功, 非 0 为对应错误码)
    private int code;
    /// 提示信息(已按当前 locale 本地化)
    private String msg;
    /// 业务数据
    private T data;
    /// 追踪ID(由 OTel 自动注入 MDC, 便于链路排障)
    private String traceId;

    /// 构建成功响应
    public static <T> DaxResult<T> ok(T data) {
        DaxResult<T> r = new DaxResult<>();
        r.code = ChannelErrorCode.SUCCESS.getCode();
        r.msg = ChannelErrorCode.SUCCESS.getMessage();
        r.data = data;
        r.traceId = MDC.get("traceId");
        return r;
    }

    /// 构建(指定 code 与提示信息的)失败响应
    public static <T> DaxResult<T> fail(int code, String msg) {
        DaxResult<T> r = new DaxResult<>();
        r.code = code;
        r.msg = msg;
        r.traceId = MDC.get("traceId");
        return r;
    }

    /// 基于 [ChannelErrorCode] 构建失败响应
    public static <T> DaxResult<T> fail(ChannelErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
