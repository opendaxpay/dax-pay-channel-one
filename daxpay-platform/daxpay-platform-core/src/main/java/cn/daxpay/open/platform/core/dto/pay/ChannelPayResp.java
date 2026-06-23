package cn.daxpay.open.platform.core.dto.pay;

import lombok.Data;

@Data
public class ChannelPayResp {
    private String bizOrderNo;
    private String outOrderNo;
    private String transOrderNo;
    private String payBody;
    private String payBodyType;
    private Boolean complete;
    private String finishTime;
}
