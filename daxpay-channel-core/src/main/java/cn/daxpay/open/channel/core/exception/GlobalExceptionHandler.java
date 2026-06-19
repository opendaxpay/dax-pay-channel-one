package cn.daxpay.open.channel.core.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.common.exception.ChannelErrorCode;
import cn.daxpay.open.channel.core.result.DaxResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChannelServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DaxResult<Void> handleChannelService(ChannelServiceException e) {
        log.error("通道服务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return DaxResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(SdkCallException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public DaxResult<Void> handleSdkCall(SdkCallException e) {
        log.error("SDK 调用异常: {}", e.getMessage());
        return DaxResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DaxResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return DaxResult.fail(ChannelErrorCode.INVALID_CONFIG.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DaxResult<Void> handleConstraintViolation(ConstraintViolationException e) {
        return DaxResult.fail(ChannelErrorCode.INVALID_CONFIG.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public DaxResult<Void> handleUnknown(Exception e) {
        log.error("系统内部异常", e);
        return DaxResult.fail(ChannelErrorCode.SYSTEM_ERROR.getCode(), ChannelErrorCode.SYSTEM_ERROR.getMessage());
    }
}
