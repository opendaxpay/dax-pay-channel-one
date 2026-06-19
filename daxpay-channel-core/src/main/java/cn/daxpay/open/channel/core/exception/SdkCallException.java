package cn.daxpay.open.channel.core.exception;

import cn.daxpay.open.channel.common.exception.ChannelErrorCode;

public class SdkCallException extends ChannelServiceException {

    public SdkCallException(String detail) {
        super(ChannelErrorCode.SDK_CALL_FAILED, detail);
    }

    public SdkCallException(String detail, Throwable cause) {
        super(ChannelErrorCode.SDK_CALL_FAILED.getCode(), ChannelErrorCode.SDK_CALL_FAILED.getMessage() + ": " + detail + " - " + cause.getMessage());
        initCause(cause);
    }
}
