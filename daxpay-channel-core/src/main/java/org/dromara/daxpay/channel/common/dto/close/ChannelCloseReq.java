package org.dromara.daxpay.channel.common.dto.close;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelCloseReq {
    @NotBlank
    private String channel;
    @NotBlank
    private String bizOrderNo;
    @NotNull
    private Map<String, Object> config;
}
