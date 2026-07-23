package cn.daxpay.open.platform.system.filter;

import cn.daxpay.open.platform.common.i18n.util.I18nUtil;
import cn.daxpay.open.platform.common.util.encrypt.ChannelAesGcmEncryptor;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/// # 通道传输报文加解密过滤器
///
/// `/channel/**` 强制 AES-GCM 解密请求、加密响应；`/actuator/**`、`/internal/**` 放行明文。
/// 顺序晚于 [TraceIdFilter]，保证 MDC/trace 头已就绪。
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelTransportEncryptFilter extends OncePerRequestFilter implements Ordered {

    /// 与主应用 WebHeaderCode.X_DAX_PAYLOAD_ENCRYPTED / Interceptor 常量一致
    public static final String HEADER_X_DAX_PAYLOAD_ENCRYPTED = "X-Dax-Payload-Encrypted";

    /// 请求未携带传输加密头
    public static final String MSG_REQUEST_HEADER_MISSING =
            "channel.error.transportEncrypt.requestHeaderMissing";

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

    private final ChannelAesGcmEncryptor encryptor;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 仅对 /channel/** 强制加解密；actuator / internal 等保持明文
        String path = request.getRequestURI();
        return !path.startsWith("/channel");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest wrappedRequest = decryptRequest(request, response);
        if (wrappedRequest == null) {
            // 已写 400
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        chain.doFilter(wrappedRequest, responseWrapper);
        encryptResponse(responseWrapper);
    }

    /// 解密请求体；失败时写 400 并返回 null
    private HttpServletRequest decryptRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        byte[] raw = StreamUtils.copyToByteArray(request.getInputStream());
        if (raw.length == 0) {
            return request;
        }

        String encryptedFlag = request.getHeader(HEADER_X_DAX_PAYLOAD_ENCRYPTED);
        if (!"true".equalsIgnoreCase(encryptedFlag)) {
            log.warn("通道传输加密：请求未携带加密头 path={}", request.getRequestURI());
            // 请求未携带传输加密头
            writeBadRequest(response, I18nUtil.get(MSG_REQUEST_HEADER_MISSING));
            return null;
        }

        try {
            String plaintext = encryptor.decrypt(new String(raw, StandardCharsets.UTF_8));
            byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            return new DecryptedRequestWrapper(request, plainBytes);
        } catch (Exception e) {
            log.warn("通道传输解密失败 path={} type={}", request.getRequestURI(), e.getClass().getSimpleName());
            // 通道传输解密失败（优先用异常上的 messageKey）
            String key = e.getMessage() != null ? e.getMessage() : ChannelAesGcmEncryptor.MSG_DECRYPT_FAILED;
            writeBadRequest(response, I18nUtil.get(key));
            return null;
        }
    }

    /// 加密响应体并写出
    private void encryptResponse(ContentCachingResponseWrapper responseWrapper) throws IOException {
        byte[] body = responseWrapper.getContentAsByteArray();
        if (body.length == 0) {
            responseWrapper.copyBodyToResponse();
            return;
        }

        String ciphertext = encryptor.encrypt(new String(body, StandardCharsets.UTF_8));
        byte[] encryptedBytes = ciphertext.getBytes(StandardCharsets.UTF_8);

        responseWrapper.resetBuffer();
        responseWrapper.setHeader(HEADER_X_DAX_PAYLOAD_ENCRYPTED, "true");
        responseWrapper.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8).toString());
        responseWrapper.setContentLength(encryptedBytes.length);
        responseWrapper.getOutputStream().write(encryptedBytes);
        responseWrapper.copyBodyToResponse();
    }

    private static void writeBadRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = "{\"code\":400,\"msg\":\"" + message + "\"}";
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }

    /// 将解密后的明文 JSON 暴露给下游 HttpMessageConverter
    static final class DecryptedRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        DecryptedRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // 非异步读取
                }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public String getContentType() {
            return MediaType.APPLICATION_JSON_VALUE;
        }

        @Override
        public String getHeader(String name) {
            if (StrUtil.equalsIgnoreCase(name, "Content-Type")) {
                return MediaType.APPLICATION_JSON_VALUE;
            }
            if (StrUtil.equalsIgnoreCase(name, "Content-Length")) {
                return String.valueOf(body.length);
            }
            return super.getHeader(name);
        }
    }
}
