package cn.daxpay.open.channel.wechat.req;

import cn.daxpay.open.channel.wechat.config.WechatSdkCredential;
import lombok.Data;

/// # 微信回调验签解析请求(与主应用镜像)
@Data
public class WechatCallbackParseReq {

    /// 通道调用凭证
    private WechatSdkCredential credential;

    /// 回调原始 body
    private String body;

    /// 回调头: Wechatpay-Serial
    private String serial;

    /// 回调头: Wechatpay-Nonce
    private String nonce;

    /// 回调头: Wechatpay-Signature
    private String signature;

    /// 回调头: Wechatpay-Timestamp
    private String timestamp;
}
