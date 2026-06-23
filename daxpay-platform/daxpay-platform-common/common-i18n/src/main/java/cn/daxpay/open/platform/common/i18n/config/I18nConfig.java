package cn.daxpay.open.platform.common.i18n.config;

import cn.daxpay.open.platform.common.i18n.source.JsonMessageSource;
import cn.daxpay.open.platform.common.i18n.util.I18nUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.Locale;

/// # 国际化配置
///
/// 注册 JsonMessageSource 作为 MessageSource 的实现,
/// Spring Boot 默认的 AcceptHeaderLocaleResolver 会根据请求头自动解析 locale, 无需额外配置 LocaleResolver。
/// 同时 Spring Boot 自动配置的 LocalValidatorFactoryBean 会用此 MessageSource 解析 Bean Validation 的 {key} 消息。
@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource(ResourcePatternResolver resourceResolver) {
        JsonMessageSource source = new JsonMessageSource(resourceResolver);
        source.setDefaultLocale(Locale.CHINA);
        // 初始化静态工具类
        I18nUtil.setMessageSource(source);
        return source;
    }
}
