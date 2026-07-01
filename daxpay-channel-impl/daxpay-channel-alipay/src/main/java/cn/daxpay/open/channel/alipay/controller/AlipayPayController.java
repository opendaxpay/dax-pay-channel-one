package cn.daxpay.open.channel.alipay.controller;

import cn.daxpay.open.channel.alipay.req.AlipayPayReq;
import cn.daxpay.open.channel.alipay.req.AlipaySyncReq;
import cn.daxpay.open.channel.alipay.resp.AlipayPayResp;
import cn.daxpay.open.channel.alipay.resp.AlipaySyncResp;
import cn.daxpay.open.channel.alipay.service.AlipayPayService;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # 支付宝通道接收接口
///
/// 接收主应用 dax-pay-open 经声明式 HTTP 客户端转发的支付宝支付请求。
/// 通道与操作各自独立端点(通道前缀 `/channel/alipay`), 不做通用分发。
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channel/alipay")
public class AlipayPayController {

    private final AlipayPayService alipayPayService;

    /// 支付下单
    @PostMapping("/pay")
    public DaxResult<AlipayPayResp> pay(@Valid @RequestBody AlipayPayReq req) {
        return DaxResult.ok(alipayPayService.pay(req));
    }

    /// 支付同步(查询支付宝订单状态)
    @PostMapping("/sync")
    public DaxResult<AlipaySyncResp> sync(@Valid @RequestBody AlipaySyncReq req) {
        return DaxResult.ok(alipayPayService.sync(req));
    }
}
