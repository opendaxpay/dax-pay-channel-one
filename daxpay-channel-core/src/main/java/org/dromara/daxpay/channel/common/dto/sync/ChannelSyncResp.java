package org.dromara.daxpay.channel.common.dto.sync;

import lombok.Data;

import java.util.Map;

@Data
public class ChannelSyncResp {
    private String bizOrderNo;
    private String outOrderNo;
    private String transOrderNo;
    private String status;
    private Long amount;
    private String finishTime;
    private String buyerId;
    private Map<String, Object> rawResponse;
}
