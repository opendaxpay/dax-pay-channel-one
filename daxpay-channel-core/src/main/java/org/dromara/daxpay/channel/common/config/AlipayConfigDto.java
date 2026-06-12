package org.dromara.daxpay.channel.common.config;

import lombok.Data;

@Data
public class AlipayConfigDto {
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String appCert;
    private String alipayCert;
    private String alipayRootCert;
    private String serverUrl;
    private String signType;
    private String authType;
    private Boolean sandbox;
    private String appAuthToken;
}
