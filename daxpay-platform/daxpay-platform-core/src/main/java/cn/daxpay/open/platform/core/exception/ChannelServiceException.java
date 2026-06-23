package cn.daxpay.open.platform.core.exception;

import cn.daxpay.open.platform.common.i18n.util.I18nUtil;
import lombok.Getter;

/// # 通道服务业务异常
///
/// 持有 i18n messageKey 与占位符参数, 构造时经 I18nUtil 解析为当前 locale 的本地化文案。
@Getter
public class ChannelServiceException extends RuntimeException {

    private final int code;
    private final String messageKey;
    private final Object[] args;

    public ChannelServiceException(ChannelErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessageKey());
    }

    public ChannelServiceException(ChannelErrorCode errorCode, Object... args) {
        this(errorCode.getCode(), errorCode.getMessageKey(), args);
    }

    /// 直接指定 code + messageKey + 占位符参数 (用于带 detail 的 WithDetail key)
    public ChannelServiceException(int code, String messageKey, Object... args) {
        super(I18nUtil.get(messageKey, args));
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
    }
}
