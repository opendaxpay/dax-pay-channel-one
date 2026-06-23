package cn.daxpay.open.platform.core.dto.refund;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelRefundReq {
    @NotBlank(message = "{validation.field.channel.notBlank}")
    private String channel;
    @NotBlank(message = "{validation.field.bizRefundOrderNo.notBlank}")
    private String bizRefundOrderNo;
    @NotBlank(message = "{validation.field.bizOrderNo.notBlank}")
    private String bizOrderNo;
    @NotNull(message = "{validation.field.amount.notNull}")
    private Long amount;
    private String reason;
    @NotNull(message = "{validation.field.config.notNull}")
    private Map<String, Object> config;
}
