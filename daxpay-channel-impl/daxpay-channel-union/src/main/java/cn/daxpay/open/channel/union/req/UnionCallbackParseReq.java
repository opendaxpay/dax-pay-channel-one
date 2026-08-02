package cn.daxpay.open.channel.union.req;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import lombok.Data;

import java.util.Map;

/// # 云闪付回调验签解析请求
///
/// 主应用接收到银联异步通知后, 将回调参数连同通道凭证转发到子应用,
/// 由子应用使用 [cn.daxpay.open.channel.union.util.UnionSignUtil] 完成证书验签,
/// 返回结构化的回调业务数据 [cn.daxpay.open.channel.union.resp.UnionCallbackParseResp]。
///
/// 设计目的: 主应用零证书依赖, 验签能力集中在 channel-one 子应用。
///
/// 银联 ACP 回调为 form 参数(Map), 含 signature 与 signPubKeyCert(银联签名证书),
/// 验签方式为 RSA2 证书公钥校验(区别于银联商务的 secretKey 拼接)。
@Data
public class UnionCallbackParseReq {

    /// 通道调用凭证(用于获取中级证书做回调验签)
    private UnionSdkCredential credential;

    /// 回调原始参数(银联异步通知的全部字段, 含 signature/signPubKeyCert)
    private Map<String, String> params;
}
