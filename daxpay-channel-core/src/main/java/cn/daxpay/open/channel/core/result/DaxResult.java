package cn.daxpay.open.channel.core.result;

import lombok.Data;
import cn.daxpay.open.channel.common.exception.ChannelErrorCode;

@Data
public class DaxResult<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> DaxResult<T> ok(T data) {
        DaxResult<T> r = new DaxResult<>();
        r.code = ChannelErrorCode.SUCCESS.getCode();
        r.msg = ChannelErrorCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> DaxResult<T> fail(int code, String msg) {
        DaxResult<T> r = new DaxResult<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> DaxResult<T> fail(ChannelErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
