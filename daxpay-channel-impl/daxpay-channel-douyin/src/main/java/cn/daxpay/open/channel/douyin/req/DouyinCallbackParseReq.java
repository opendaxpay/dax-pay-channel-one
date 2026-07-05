package cn.daxpay.open.channel.douyin.req;

import cn.daxpay.open.channel.douyin.config.DouyinSdkCredential;
import lombok.Data;

/// # 抖音回调验签解析请求
///
/// 主应用接收到抖音异步通知后, 将原始 header + body 连同通道凭证转发到子应用,
/// 由子应用使用 [com.douyinpay.api.notification.NotificationParser] 完成验签与 AES 解密,
/// 返回结构化的回调业务数据 [cn.daxpay.open.channel.douyin.resp.DouyinCallbackParseResp]。
///
/// 设计目的: 主应用零 SDK 依赖, 验签能力集中在 channel-one 子应用。
@Data
public class DouyinCallbackParseReq {

    /// 通道调用凭证(用于构建 NotificationParser, 拉取平台证书验签)
    private DouyinSdkCredential credential;

    /// 回调原始 body(抖音 POST 的 JSON 密文)
    private String body;

    /// 回调头: Douyinpay-Serial(平台证书序列号)
    private String serial;

    /// 回调头: Douyinpay-Nonce(随机串)
    private String nonce;

    /// 回调头: Douyinpay-Signature(签名)
    private String signature;

    /// 回调头: Douyinpay-Timestamp(时间戳)
    private String timestamp;
}
