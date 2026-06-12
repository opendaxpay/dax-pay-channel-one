package org.dromara.daxpay.channel.common.dto.callback;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelCallbackVerifyReq {
    @NotBlank
    private String channel;
    @NotBlank
    private String callbackType;
    @NotNull
    private Map<String, String> rawParams;
    @NotNull
    private Map<String, Object> config;
}
