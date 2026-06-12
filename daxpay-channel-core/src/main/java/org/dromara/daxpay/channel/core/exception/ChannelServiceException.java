package org.dromara.daxpay.channel.core.exception;

import lombok.Getter;
import org.dromara.daxpay.channel.common.exception.ChannelErrorCode;

@Getter
public class ChannelServiceException extends RuntimeException {

    private final int code;

    public ChannelServiceException(ChannelErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public ChannelServiceException(ChannelErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
    }

    public ChannelServiceException(int code, String message) {
        super(message);
        this.code = code;
    }
}
