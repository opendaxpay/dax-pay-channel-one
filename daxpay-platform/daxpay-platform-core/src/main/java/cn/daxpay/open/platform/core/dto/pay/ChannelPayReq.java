package cn.daxpay.open.platform.core.dto.pay;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelPayReq {
    @NotBlank(message = "{validation.field.channel.notBlank}")
    private String channel;
    @NotBlank(message = "{validation.field.bizOrderNo.notBlank}")
    private String bizOrderNo;
    @NotNull(message = "{validation.field.amount.notNull}")
    private Long amount;
    @NotBlank(message = "{validation.field.subject.notBlank}")
    private String subject;
    private String description;
    @NotBlank(message = "{validation.field.method.notBlank}")
    private String method;
    private String expireTime;
    private String otherMethod;
    @NotNull(message = "{validation.field.config.notNull}")
    private Map<String, Object> config;
}
