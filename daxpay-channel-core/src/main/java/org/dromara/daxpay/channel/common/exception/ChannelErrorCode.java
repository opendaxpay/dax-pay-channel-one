package org.dromara.daxpay.channel.common.exception;

import lombok.Getter;

@Getter
public enum ChannelErrorCode {
    SUCCESS(0, "success"),
    CHANNEL_NOT_FOUND(10001, "不支持的通道编码"),
    INVALID_CONFIG(10002, "通道配置无效"),
    SDK_CALL_FAILED(10003, "SDK 调用失败"),
    CALLBACK_VERIFY_FAILED(10004, "回调验签失败"),
    CACHE_CONCURRENT_CONFLICT(10005, "并发冲突"),
    SYSTEM_ERROR(10006, "系统内部错误");

    private final int code;
    private final String message;

    ChannelErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
