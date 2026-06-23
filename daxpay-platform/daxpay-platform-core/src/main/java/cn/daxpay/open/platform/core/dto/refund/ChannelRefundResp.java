package cn.daxpay.open.platform.core.dto.refund;

import lombok.Data;

@Data
public class ChannelRefundResp {
    private String bizRefundOrderNo;
    private String outRefundOrderNo;
    private Boolean complete;
    private String finishTime;
}
