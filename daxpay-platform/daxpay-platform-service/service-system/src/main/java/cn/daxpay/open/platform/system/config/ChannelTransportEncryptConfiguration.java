package cn.daxpay.open.platform.system.config;

import cn.daxpay.open.platform.common.i18n.util.I18nUtil;
import cn.daxpay.open.platform.common.util.encrypt.ChannelAesGcmEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/// # 通道传输加密 Spring 配置
///
/// 传输加密强制常开：启动时校验 key，注册 [ChannelAesGcmEncryptor] Bean。
@Slf4j
@Configuration
@EnableConfigurationProperties(ChannelTransportEncryptProperties.class)
public class ChannelTransportEncryptConfiguration {

    @Bean
    public ChannelAesGcmEncryptor channelAesGcmEncryptor(ChannelTransportEncryptProperties properties) {
        try {
            ChannelAesGcmEncryptor encryptor = new ChannelAesGcmEncryptor(properties.getKey());
            log.info("通道传输加密已加载（强制常开）");
            return encryptor;
        } catch (IllegalArgumentException e) {
            // 通道传输加密密钥长度非法
            String msg = I18nUtil.get(ChannelAesGcmEncryptor.MSG_KEY_INVALID, Locale.CHINA,
                    ChannelAesGcmEncryptor.KEY_LENGTH);
            throw new IllegalStateException(msg, e);
        }
    }
}
