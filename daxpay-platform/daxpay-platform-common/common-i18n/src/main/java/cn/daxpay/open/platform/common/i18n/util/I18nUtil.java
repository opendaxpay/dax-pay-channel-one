package cn.daxpay.open.platform.common.i18n.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/// # 国际化工具类
///
/// 提供静态方法获取翻译文本, 自动从 LocaleContextHolder 获取当前请求的语言
/// (Spring Boot 默认的 AcceptHeaderLocaleResolver 会根据请求头 Accept-Language 解析)
public final class I18nUtil {

    private static MessageSource messageSource;

    private I18nUtil() {
    }

    /// 设置 MessageSource, 由 I18nConfig 在启动时调用
    public static void setMessageSource(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }

    /// 获取翻译文本
    /// @param code 消息 key (如 channel.error.channelNotFound)
    /// @param args 消息参数 (可选, 对应 JSON 中的 {0}、{1} 占位符)
    /// @return 翻译后的文本; 若 messageSource 未初始化或 key 不存在则返回 code 本身
    public static String get(String code, Object... args) {
        if (messageSource == null) {
            return code;
        }
        var locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(code, args, code, locale);
        }
        catch (Exception e) {
            return code;
        }
    }
}
