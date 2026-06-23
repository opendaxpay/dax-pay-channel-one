package cn.daxpay.open.channel.controller;

import cn.daxpay.open.platform.core.dto.pay.ChannelPayReq;
import cn.daxpay.open.platform.core.dto.pay.ChannelPayResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.result.DaxResult;
import cn.daxpay.open.platform.core.service.ChannelPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/// # 通道支付入口 Controller (临时演示代码, 验证后删除)
///
/// 接收主应用 dax-pay-open 通过 `AlipayChannelClient`(`@PostExchange("/channel/pay")`)
/// 转发来的通道支付请求, 按 `channel` 字段路由到对应的 `ChannelPayService` 实现。
///
/// mock 策略: 主应用 Demo 入口传 `config=空Map`, 各通道 Service 进入 Demo 模式返回模拟响应, 不读数据库。
@Slf4j
@Validated
@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelPayController {

    // Spring 按 bean 名收集所有 ChannelPayService 实现: {"alipay"=AlipayPayService, "wechat"=WechatPayService}
    private final Map<String, ChannelPayService> payServices;

    /// 通道支付下单
    ///
    /// 请求体 `ChannelPayReq` 带 Bean Validation 注解, 校验失败由 GlobalExceptionHandler 统一处理。
    @PostMapping("/pay")
    public DaxResult<ChannelPayResp> pay(@Validated @RequestBody ChannelPayReq req) {
        log.info("接收通道支付请求: channel={}, bizOrderNo={}, amount={}, method={}",
                req.getChannel(), req.getBizOrderNo(), req.getAmount(), req.getMethod());

        ChannelPayService service = payServices.get(req.getChannel());

        // 不支持的通道编码
        if (service == null) {
            throw new ChannelServiceException(ChannelErrorCode.CHANNEL_NOT_FOUND, req.getChannel());
        }

        return DaxResult.ok(service.pay(req));
    }
}
