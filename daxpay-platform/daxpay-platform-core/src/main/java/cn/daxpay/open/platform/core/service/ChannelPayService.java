package cn.daxpay.open.platform.core.service;

import cn.daxpay.open.platform.core.dto.pay.ChannelPayReq;
import cn.daxpay.open.platform.core.dto.pay.ChannelPayResp;

/// # 通道支付服务
///
/// 各支付通道(支付宝/微信等)实现本接口, 由 Controller 按 `channel` 字段路由调用。
/// 实现类需以通道编码作为 Spring bean 名(如 `@Service("alipay")`)。
public interface ChannelPayService {

    /// 通道支付下单
    ///
    /// 根据请求中的支付方式(method)调用对应三方接口, 返回支付凭证(链接/二维码/表单等)。
    /// 当 `config` 为空时进入 Demo 模式, 返回模拟响应, 不调用真实 SDK。
    ChannelPayResp pay(ChannelPayReq req);
}
