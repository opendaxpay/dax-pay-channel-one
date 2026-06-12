package org.dromara.daxpay.channel.common.dto.close;

import lombok.Data;

@Data
public class ChannelCloseResp {
    private String bizOrderNo;
    private String result;
}
