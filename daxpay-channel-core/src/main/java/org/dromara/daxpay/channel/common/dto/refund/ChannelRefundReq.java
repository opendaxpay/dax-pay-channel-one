package org.dromara.daxpay.channel.common.dto.refund;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelRefundReq {
    @NotBlank
    private String channel;
    @NotBlank
    private String bizRefundOrderNo;
    @NotBlank
    private String bizOrderNo;
    @NotNull
    private Long amount;
    private String reason;
    @NotNull
    private Map<String, Object> config;
}
