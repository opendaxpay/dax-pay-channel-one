package cn.daxpay.open.channel.alipay.req;

import cn.daxpay.open.channel.alipay.config.AlipaySdkCredential;
import lombok.Data;

import java.util.Map;

/// # 支付宝回调验签解析请求
///
/// 主应用接收到支付宝异步通知后, 将原始表单参数(params)连同通道凭证转发到子应用,
/// 由子应用使用 [com.alipay.api.internal.util.AlipaySignature] 完成验签,
/// 返回结构化的回调业务数据 [cn.daxpay.open.channel.alipay.resp.AlipayCallbackParseResp]。
///
/// 设计目的: 主应用零 SDK 依赖, 验签能力集中在 channel-one 子应用。
@Data
public class AlipayCallbackParseReq {

    /// 通道调用凭证(用于获取支付宝公钥/证书进行验签)
    private AlipaySdkCredential credential;

    /// 回调原始表单参数(支付宝异步通知全部 form 参数, 含 sign / sign_type)
    private Map<String, String> params;
}
