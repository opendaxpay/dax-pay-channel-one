package cn.daxpay.open.channel.ums.req;

import cn.daxpay.open.channel.ums.config.UmsSdkCredential;
import lombok.Data;

import java.util.Map;

/// # 银联商务回调验签解析请求
///
/// 主应用接收到银联商务异步通知后, 将回调参数连同通道凭证转发到子应用,
/// 由子应用使用 [cn.daxpay.open.channel.ums.util.UmsSignUtil] 完成验签,
/// 返回结构化的回调业务数据 [cn.daxpay.open.channel.ums.resp.UmsCallbackParseResp]。
///
/// 设计目的: 主应用零签名依赖, 验签能力集中在 channel-one 子应用。
///
/// 银联商务回调为 form 参数(Map), 与抖音的 header+body 方式不同。
@Data
public class UmsCallbackParseReq {

    /// 通道调用凭证(用于获取 secretKey 做回调验签)
    private UmsSdkCredential credential;

    /// 回调原始参数(银联商务异步通知的全部字段, 含 sign/signType)
    private Map<String, String> params;
}
