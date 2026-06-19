package cn.daxpay.open.channel.common.dto.pay;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelPayReq {
    @NotBlank
    private String channel;
    @NotBlank
    private String bizOrderNo;
    @NotNull
    private Long amount;
    @NotBlank
    private String subject;
    private String description;
    @NotBlank
    private String method;
    private String expireTime;
    private String otherMethod;
    @NotNull
    private Map<String, Object> config;
}
