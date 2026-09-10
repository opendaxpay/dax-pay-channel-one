package cn.daxpay.open.platform.common.util.encrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/// # 通道传输 AES-256-GCM 加密器（单密钥、无版本前缀）
///
/// 与主应用 `cn.daxpay.open.platform.common.config.encrypt.ChannelAesGcmEncryptor` 协议一致：
/// `Base64(IV(12) || ciphertext+tag)`。子应用独立工程无共享 jar，故本地保留同源实现。
///
/// 异常 message 使用 i18n messageKey（由调用方经 [I18nUtil] 解析），不写死中文。
public class ChannelAesGcmEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    /// 密钥须为恰好 32 个 UTF-8 字符（AES-256）
    public static final int KEY_LENGTH = 32;

    /// 通道传输加密密钥长度非法
    public static final String MSG_KEY_INVALID = "channel.error.transportEncrypt.keyInvalid";
    /// 通道传输加密失败
    public static final String MSG_ENCRYPT_FAILED = "channel.error.transportEncrypt.encryptFailed";
    /// 通道传输密文长度非法
    public static final String MSG_CIPHERTEXT_INVALID = "channel.error.transportEncrypt.ciphertextInvalid";
    /// 通道传输解密失败
    public static final String MSG_DECRYPT_FAILED = "channel.error.transportEncrypt.decryptFailed";

    private final SecretKey secretKey;

    /// @param key AES-256 密钥，恰好 32 字符
    public ChannelAesGcmEncryptor(String key) {
        validateKey(key);
        this.secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /// 校验密钥长度
    public static void validateKey(String key) {
        if (Objects.isNull(key) || key.length() != KEY_LENGTH) {
            // 通道传输加密密钥长度非法（message 为 i18n key，参数由调用方传入 I18nUtil）
            throw new IllegalArgumentException(MSG_KEY_INVALID);
        }
    }

    /// 加密明文
    public String encrypt(String plaintext) {
        if (Objects.isNull(plaintext)) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // 通道传输加密失败
            throw new IllegalStateException(MSG_ENCRYPT_FAILED, e);
        }
    }

    /// 解密密文
    public String decrypt(String ciphertext) {
        if (Objects.isNull(ciphertext)) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length <= GCM_IV_LENGTH) {
                // 通道传输密文长度非法
                throw new IllegalStateException(MSG_CIPHERTEXT_INVALID);
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // 通道传输解密失败
            throw new IllegalStateException(MSG_DECRYPT_FAILED, e);
        }
    }
}
