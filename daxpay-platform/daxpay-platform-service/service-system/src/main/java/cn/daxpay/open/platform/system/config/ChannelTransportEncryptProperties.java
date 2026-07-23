package cn.daxpay.open.platform.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// # 通道传输加密配置（强制常开）
///
/// 与主应用 `daxpay.channel.one.transport-encrypt.key` 保持一致。
@Data
@ConfigurationProperties(prefix = "daxpay.channel.transport-encrypt")
public class ChannelTransportEncryptProperties {

    /// AES-256 密钥，恰好 32 个 UTF-8 字符
    private String key;
}
