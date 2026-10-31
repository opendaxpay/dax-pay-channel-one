package cn.daxpay.open.channel.mock.req;

import cn.daxpay.open.channel.mock.config.MockSdkCredential;
import lombok.Data;

/// # Mock 通道回调解析请求(主应用 → one)
///
/// 定稿形态: credential + body + 签名三值(sign/timestamp/nonce)共五字段。
/// body 为 10400 回调原始 JSON body 原文(明文单层 event_type/data 结构),
/// sign/timestamp/nonce 为回调签名三头(Mock-Sign/Mock-Timestamp/Mock-Nonce)原值,
/// 均由主应用回调接收端原样转发; one 用凭证平台公钥对三行签名串验签后取 data 解析,
/// 支付/退款回调共用本形态。
@Data
public class MockCallbackParseReq {

    /// 通道调用凭证(验签密钥素材)
    private MockSdkCredential credential;

    /// 回调原始 JSON body 原文(明文单层 event_type/data 结构)
    private String body;

    /// 回调签名值(Base64 RSA-SHA256, Mock-Sign 头原值)
    private String sign;

    /// 秒级时间戳(Mock-Timestamp 头原值)
    private String timestamp;

    /// 32 位随机 hex(Mock-Nonce 头原值)
    private String nonce;
}
