package cn.daxpay.open.channel.alipay.controller;

import cn.daxpay.open.channel.alipay.dto.AlipayCallbackVerifyReq;
import cn.daxpay.open.channel.alipay.dto.AlipayCallbackVerifyResp;
import cn.daxpay.open.channel.alipay.service.AlipayCallbackVerifyService;
import cn.daxpay.open.platform.core.result.DaxResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # 支付宝回调验签 Controller
///
/// 接收主应用 dax-pay-open 通过 `AlipayChannelClient`(`@PostExchange("/channel/alipay/callback/verify")`)
/// 转发来的支付宝异步通知原始参数, 委托 [AlipayCallbackVerifyService] 完成 RSA2 验签与业务字段解析。
///
/// 主应用侧在收到支付宝异步通知后, 组装 [AlipayCallbackVerifyReq] 调用本接口,
/// 子应用仅负责"验签 + 解析", 不持久化订单状态(订单状态由主应用统一维护)。
@Slf4j
@Validated
@RestController
@RequestMapping("/channel/alipay/callback")
@RequiredArgsConstructor
public class AlipayCallbackController {

    private final AlipayCallbackVerifyService alipayCallbackVerifyService;

    /// 支付宝回调验签
    ///
    /// 请求体 [AlipayCallbackVerifyReq] 带 Bean Validation 注解, 校验失败由 GlobalExceptionHandler 统一处理。
    @PostMapping("/verify")
    public DaxResult<AlipayCallbackVerifyResp> verify(@Validated @RequestBody AlipayCallbackVerifyReq req) {
        log.info("接收支付宝回调验签请求: callbackType={}", req.getCallbackType());
        return DaxResult.ok(alipayCallbackVerifyService.verify(req));
    }
}
