package cn.daxpay.open.platform.system.handler.exception;

import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/// # 通道服务全局异常处理
///
/// 对标主项目 `RestExceptionHandler` 的精简版, 拦截通道服务 Web 层异常并统一封装为 [DaxResult]。
/// 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册的
/// [ChannelServiceWebAutoConfig] 以 `@Bean` 方式装配, 不依赖启动类组件扫描。
@Slf4j
@RestControllerAdvice
public class ChannelExceptionHandler {

    /// 通道业务异常(含 [SdkCallException] 子类)
    ///
    /// 消息已在异常构造时经 I18nUtil 本地化, 直接透传 code 与 message。
    @ExceptionHandler(ChannelServiceException.class)
    public DaxResult<Void> handleChannelServiceException(ChannelServiceException ex) {
        log.info(ex.getMessage());
        return DaxResult.fail(ex.getCode(), ex.getMessage());
    }

    /// 请求体参数校验未通过(@RequestBody + @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public DaxResult<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.info(ex.getMessage());
        StringBuilder message = new StringBuilder();
        for (var error : ex.getAllErrors()) {
            message.append(error.getDefaultMessage()).append(System.lineSeparator());
        }
        return DaxResult.fail(ChannelErrorCode.VALIDATE_PARAMS.getCode(), message.toString());
    }

    /// 表单参数绑定/校验未通过
    @ExceptionHandler(BindException.class)
    public DaxResult<Void> handleBindException(BindException ex) {
        log.info(ex.getMessage());
        StringBuilder message = new StringBuilder();
        for (var error : ex.getBindingResult().getAllErrors()) {
            message.append(error.getDefaultMessage()).append(System.lineSeparator());
        }
        return DaxResult.fail(ChannelErrorCode.VALIDATE_PARAMS.getCode(), message.toString());
    }

    /// 约束违反校验未通过(PathVariable / RequestParam 参数校验)
    @ExceptionHandler(ConstraintViolationException.class)
    public DaxResult<Void> handleConstraintViolation(ConstraintViolationException ex) {
        log.info(ex.getMessage());
        StringBuilder message = new StringBuilder();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            message.append(violation.getMessage()).append(System.lineSeparator());
        }
        return DaxResult.fail(ChannelErrorCode.VALIDATE_PARAMS.getCode(), message.toString());
    }

    /// 其他校验异常兜底
    @ExceptionHandler(ValidationException.class)
    public DaxResult<Void> handleValidation(ValidationException ex) {
        log.info(ex.getMessage());
        return DaxResult.fail(ChannelErrorCode.VALIDATE_PARAMS.getCode(), ex.getMessage());
    }

    /// 运行时异常兜底
    @ExceptionHandler(RuntimeException.class)
    public DaxResult<Void> handleRuntimeException(RuntimeException ex) {
        log.error(ex.getMessage(), ex);
        return DaxResult.fail(ChannelErrorCode.SYSTEM_ERROR);
    }

    /// 最终兜底
    @ExceptionHandler(Throwable.class)
    public DaxResult<Void> handleThrowable(Throwable ex) {
        log.error(ex.getMessage(), ex);
        return DaxResult.fail(ChannelErrorCode.SYSTEM_ERROR);
    }
}
