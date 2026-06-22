package cn.daxpay.open.channel.core.result;

import cn.daxpay.open.channel.common.exception.ChannelErrorCode;
import lombok.Data;
import org.slf4j.MDC;

@Data
public class DaxResult<T> {
    private int code;
    private String msg;
    private T data;
    /// 追踪ID(由 OTel 自动注入 MDC, 便于链路排障)
    private String traceId;

    public static <T> DaxResult<T> ok(T data) {
        DaxResult<T> r = new DaxResult<>();
        r.code = ChannelErrorCode.SUCCESS.getCode();
        r.msg = ChannelErrorCode.SUCCESS.getMessage();
        r.data = data;
        r.traceId = MDC.get("traceId");
        return r;
    }

    public static <T> DaxResult<T> fail(int code, String msg) {
        DaxResult<T> r = new DaxResult<>();
        r.code = code;
        r.msg = msg;
        r.traceId = MDC.get("traceId");
        return r;
    }

    public static <T> DaxResult<T> fail(ChannelErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
