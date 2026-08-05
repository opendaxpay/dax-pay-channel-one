package cn.daxpay.open.channel.alipay.controller;

import cn.daxpay.open.channel.alipay.req.AlipayAppAuthTokenReq;
import cn.daxpay.open.channel.alipay.req.AlipayCallbackParseReq;
import cn.daxpay.open.channel.alipay.req.AlipayCloseReq;
import cn.daxpay.open.channel.alipay.req.AlipayPayReq;
import cn.daxpay.open.channel.alipay.req.AlipayRefundReq;
import cn.daxpay.open.channel.alipay.req.AlipayRefundSyncReq;
import cn.daxpay.open.channel.alipay.req.AlipaySyncReq;
import cn.daxpay.open.channel.alipay.req.AlipayTransferReq;
import cn.daxpay.open.channel.alipay.resp.AlipayAppAuthTokenResp;
import cn.daxpay.open.channel.alipay.resp.AlipayCallbackParseResp;
import cn.daxpay.open.channel.alipay.resp.AlipayCloseResp;
import cn.daxpay.open.channel.alipay.resp.AlipayPayResp;
import cn.daxpay.open.channel.alipay.resp.AlipayRefundResp;
import cn.daxpay.open.channel.alipay.resp.AlipayRefundSyncResp;
import cn.daxpay.open.channel.alipay.resp.AlipaySyncResp;
import cn.daxpay.open.channel.alipay.resp.AlipayTransferResp;
import cn.daxpay.open.channel.alipay.service.auth.AlipayAppAuthTokenService;
import cn.daxpay.open.channel.alipay.service.callback.AlipayCallbackParseService;
import cn.daxpay.open.channel.alipay.service.close.AlipayCloseService;
import cn.daxpay.open.channel.alipay.service.pay.AlipayPayService;
import cn.daxpay.open.channel.alipay.service.refund.AlipayRefundService;
import cn.daxpay.open.channel.alipay.service.refund.AlipayRefundSyncService;
import cn.daxpay.open.channel.alipay.service.sync.AlipaySyncService;
import cn.daxpay.open.channel.alipay.service.transfer.AlipayTransferService;
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
    private final AlipaySyncService alipaySyncService;
    private final AlipayCloseService alipayCloseService;
    private final AlipayRefundService alipayRefundService;
    private final AlipayRefundSyncService alipayRefundSyncService;
    private final AlipayTransferService alipayTransferService;
    private final AlipayCallbackParseService alipayCallbackParseService;
    private final AlipayAppAuthTokenService alipayAppAuthTokenService;

    /// 支付下单
    @PostMapping("/pay")
    public DaxResult<AlipayPayResp> pay(@Valid @RequestBody AlipayPayReq req) {
        return DaxResult.ok(alipayPayService.pay(req));
    }

    /// 支付同步(查询支付宝订单状态)
    @PostMapping("/sync")
    public DaxResult<AlipaySyncResp> sync(@Valid @RequestBody AlipaySyncReq req) {
        return DaxResult.ok(alipaySyncService.sync(req));
    }

    /// 关闭/撤销支付宝订单
    @PostMapping("/close")
    public DaxResult<AlipayCloseResp> close(@Valid @RequestBody AlipayCloseReq req) {
        return DaxResult.ok(alipayCloseService.close(req));
    }

    /// 退款
    @PostMapping("/refund")
    public DaxResult<AlipayRefundResp> refund(@Valid @RequestBody AlipayRefundReq req) {
        return DaxResult.ok(alipayRefundService.refund(req));
    }

    /// 退款同步(查询退款状态)
    @PostMapping("/refund-sync")
    public DaxResult<AlipayRefundSyncResp> refundSync(@Valid @RequestBody AlipayRefundSyncReq req) {
        return DaxResult.ok(alipayRefundSyncService.sync(req));
    }

    /// 转账(单笔转账)
    @PostMapping("/transfer")
    public DaxResult<AlipayTransferResp> transfer(@Valid @RequestBody AlipayTransferReq req) {
        return DaxResult.ok(alipayTransferService.transfer(req));
    }

    /// 转账同步(查询转账状态)
    @PostMapping("/transfer-sync")
    public DaxResult<AlipayTransferResp> transferSync(@Valid @RequestBody AlipayTransferReq req) {
        return DaxResult.ok(alipayTransferService.sync(req));
    }

    /// 支付回调验签解析(主应用转发)
    @PostMapping("/callback/parse-pay")
    public DaxResult<AlipayCallbackParseResp> parsePayCallback(@RequestBody AlipayCallbackParseReq req) {
        return DaxResult.ok(alipayCallbackParseService.parsePay(req));
    }

    /// 退款回调验签解析(主应用转发)
    @PostMapping("/callback/parse-refund")
    public DaxResult<AlipayCallbackParseResp> parseRefundCallback(@RequestBody AlipayCallbackParseReq req) {
        return DaxResult.ok(alipayCallbackParseService.parseRefund(req));
    }

    /// 应用授权码换取 app_auth_token(代运营授权)
    @PostMapping("/auth/app-token")
    public DaxResult<AlipayAppAuthTokenResp> exchangeAppAuthToken(@Valid @RequestBody AlipayAppAuthTokenReq req) {
        return DaxResult.ok(alipayAppAuthTokenService.exchange(req));
    }
}
