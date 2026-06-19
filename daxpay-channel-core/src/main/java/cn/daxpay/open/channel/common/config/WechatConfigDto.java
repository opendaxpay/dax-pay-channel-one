package cn.daxpay.open.channel.common.config;

import lombok.Data;

@Data
public class WechatConfigDto {
    private String wxMchId;
    private String wxAppId;
    private String apiKeyV3;
    private String privateKey;
    private String privateCert;
    private String certSerialNo;
    private String subMchId;
    private String subAppId;
}
