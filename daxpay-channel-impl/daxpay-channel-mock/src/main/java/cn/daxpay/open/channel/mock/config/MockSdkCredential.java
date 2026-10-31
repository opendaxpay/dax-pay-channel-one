package cn.daxpay.open.channel.mock.config;

import lombok.Data;

/// # Mock 通道调用凭证
///
/// 与主应用 MockSdkCredential 镜像, 字段对齐。
/// one 侧无状态: 密钥素材全部由主应用凭证每次请求下发, 不落任何本地配置。
/// 既有业务标记(sandbox/isvNo)之上扩展密钥四件套:
/// 商户私钥(请求签名)、平台公钥(响应/回调验签)、密钥主体编号(主体级独立密钥时填充,
/// 经请求报文/查询串的 key_owner_no 字段透传 10400 按主体编号定位公钥, 直连/全局密钥时空且不携带)、
/// 服务商接入网关地址(服务商级独立密钥时可空填充, 非空优先选址, 空回落 yml base-url)。
@Data
public class MockSdkCredential {

    /// 是否沙箱环境(业务标记, mock 恒为 true 语义)
    private Boolean sandbox;

    /// 服务商编号(MISV+16位数字, 仅服务商产品填充; 直连产品为空)
    private String isvNo;

    /// 商户私钥(PKCS#8 PEM 原文, 请求签名 RSA-SHA256)
    private String merchantPrivateKey;

    /// 平台公钥(SPKI PEM 原文, 响应/回调签名验证)
    private String platformPublicKey;

    /// 密钥主体编号(仅使用主体级独立密钥时非空; 直连=通道商户号, 服务商=服务商编号;
    /// 非空时随请求报文/查询串携带 key_owner_no, 10400 按主体编号查公钥登记表验签, 空时按全局公钥验签)
    private String keyOwnerNo;

    /// 服务商接入网关地址(仅服务商级独立密钥时填充, 可空; 非空优先选址, 空回落 yml base-url)
    private String gatewayBaseUrl;
}
