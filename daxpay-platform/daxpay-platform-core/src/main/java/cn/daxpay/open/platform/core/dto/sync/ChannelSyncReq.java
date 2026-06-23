package cn.daxpay.open.platform.core.dto.sync;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelSyncReq {
    @NotBlank(message = "{validation.field.channel.notBlank}")
    private String channel;
    @NotBlank(message = "{validation.field.bizOrderNo.notBlank}")
    private String bizOrderNo;
    @NotBlank(message = "{validation.field.syncType.notBlank}")
    private String syncType;
    @NotNull(message = "{validation.field.config.notNull}")
    private Map<String, Object> config;
}
