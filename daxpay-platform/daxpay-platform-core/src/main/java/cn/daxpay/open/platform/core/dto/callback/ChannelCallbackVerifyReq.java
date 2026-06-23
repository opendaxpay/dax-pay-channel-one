package cn.daxpay.open.platform.core.dto.callback;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelCallbackVerifyReq {
    @NotBlank(message = "{validation.field.channel.notBlank}")
    private String channel;
    @NotBlank(message = "{validation.field.callbackType.notBlank}")
    private String callbackType;
    @NotNull(message = "{validation.field.rawParams.notNull}")
    private Map<String, String> rawParams;
    @NotNull(message = "{validation.field.config.notNull}")
    private Map<String, Object> config;
}
