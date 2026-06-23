package cn.daxpay.open.platform.core.dto.close;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelCloseReq {
    @NotBlank(message = "{validation.field.channel.notBlank}")
    private String channel;
    @NotBlank(message = "{validation.field.bizOrderNo.notBlank}")
    private String bizOrderNo;
    @NotNull(message = "{validation.field.config.notNull}")
    private Map<String, Object> config;
}
