package org.dromara.daxpay.channel.common.dto.sync;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelSyncReq {
    @NotBlank
    private String channel;
    @NotBlank
    private String bizOrderNo;
    @NotBlank
    private String syncType;
    @NotNull
    private Map<String, Object> config;
}
