package cn.daxpay.open.channel.exception;

import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.result.DaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/// # 全局异常处理 (临时演示代码, 验证后删除)
///
/// 统一把异常转换为 [DaxResult] 响应体, 保持与主应用 dax-pay-open 一致的错误格式,
/// 主应用声明式客户端只凭响应体中的 `code` 字段判断成败, 故始终返回 HTTP 200。
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /// 通道业务异常 (SdkCallException 继承自本异常, 由本处理器一并捕获)
    @ExceptionHandler(ChannelServiceException.class)
    public DaxResult<Void> handleChannelService(ChannelServiceException e) {
        log.warn("通道业务异常: code={}, messageKey={}", e.getCode(), e.getMessageKey());
        return DaxResult.fail(e.getCode(), e.getMessage());
    }

    /// Bean Validation 参数校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public DaxResult<Void> handleValidation(MethodArgumentNotValidException e) {
        // 拼接各字段校验错误, 便于前端定位
        String detail = e.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", detail);
        return DaxResult.fail(ChannelErrorCode.VALIDATE_PARAMS.getCode(),
                ChannelErrorCode.VALIDATE_PARAMS.getMessage() + ": " + detail);
    }

    /// 未知异常兜底
    @ExceptionHandler(Exception.class)
    public DaxResult<Void> handleException(Exception e) {
        log.error("系统未知异常", e);
        return DaxResult.fail(ChannelErrorCode.SYSTEM_ERROR);
    }
}
