package cn.daxpay.open.platform.core.exception;

/// # SDK 调用异常
///
/// 调用第三方支付 SDK 失败时抛出, 消息走 channel.error.sdkCallFailedWithDetail (带 {0} 占位符)。
public class SdkCallException extends ChannelServiceException {

    public SdkCallException(String detail) {
        super(ChannelErrorCode.SDK_CALL_FAILED.getCode(), "channel.error.sdkCallFailedWithDetail", detail);
    }

    public SdkCallException(String detail, Throwable cause) {
        super(ChannelErrorCode.SDK_CALL_FAILED.getCode(), "channel.error.sdkCallFailedWithDetail", detail + " - " + cause.getMessage());
        initCause(cause);
    }
}
