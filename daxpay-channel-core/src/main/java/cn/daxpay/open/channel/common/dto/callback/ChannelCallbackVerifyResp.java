package cn.daxpay.open.channel.common.dto.callback;

import lombok.Data;

import java.util.Map;

@Data
public class ChannelCallbackVerifyResp {
    private Boolean verified;
    private String bizOrderNo;
    private String outOrderNo;
    private String status;
    private Long amount;
    private String finishTime;
    private String buyerId;
    private Map<String, Object> rawData;
}
