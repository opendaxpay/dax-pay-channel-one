package cn.daxpay.open.platform.core.exception;

import cn.daxpay.open.platform.common.i18n.util.I18nUtil;
import lombok.Getter;

/// # 通道服务错误码
///
/// code 为业务错误码, messageKey 对应 i18n 资源 channel/error.json 中的 key (channel.error.xxx)。
/// 中文文案见 i18n/zh-CN/channel/error.json, 英文见 en-US。
@Getter
public enum ChannelErrorCode {

    SUCCESS(0, "channel.error.success"),
    /// 不支持的通道编码
    CHANNEL_NOT_FOUND(10001, "channel.error.channelNotFound"),
    /// 通道配置无效
    INVALID_CONFIG(10002, "channel.error.invalidConfig"),
    /// SDK 调用失败
    SDK_CALL_FAILED(10003, "channel.error.sdkCallFailed"),
    /// 回调验签失败
    CALLBACK_VERIFY_FAILED(10004, "channel.error.callbackVerifyFailed"),
    /// 响应验签失败
    RESPONSE_VERIFY_FAILED(10008, "channel.error.responseVerifyFailed"),
    /// 并发冲突
    CACHE_CONCURRENT_CONFLICT(10005, "channel.error.cacheConcurrentConflict"),
    /// 系统内部错误
    SYSTEM_ERROR(10006, "channel.error.systemError"),
    /// 参数校验失败
    VALIDATE_PARAMS(10007, "channel.error.validateParams"),
    /// 结果未知(用户支付中/付款码已使用/订单已支付等, 需主应用查单确认最终状态)
    RESULT_UNKNOWN(10009, "channel.error.resultUnknown");

    private final int code;
    private final String messageKey;

    ChannelErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    /// 获取当前 locale 下的本地化消息
    public String getMessage() {
        return I18nUtil.get(messageKey);
    }
}
