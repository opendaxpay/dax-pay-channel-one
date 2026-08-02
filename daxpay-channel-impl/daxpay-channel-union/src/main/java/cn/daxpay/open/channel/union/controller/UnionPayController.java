package cn.daxpay.open.channel.union.controller;

import cn.daxpay.open.channel.union.req.UnionCallbackParseReq;
import cn.daxpay.open.channel.union.req.UnionCloseReq;
import cn.daxpay.open.channel.union.req.UnionPayReq;
import cn.daxpay.open.channel.union.req.UnionRefundReq;
import cn.daxpay.open.channel.union.req.UnionRefundSyncReq;
import cn.daxpay.open.channel.union.req.UnionSyncReq;
import cn.daxpay.open.channel.union.resp.UnionCallbackParseResp;
import cn.daxpay.open.channel.union.resp.UnionCloseResp;
import cn.daxpay.open.channel.union.resp.UnionPayResp;
import cn.daxpay.open.channel.union.resp.UnionRefundResp;
import cn.daxpay.open.channel.union.resp.UnionRefundSyncResp;
import cn.daxpay.open.channel.union.resp.UnionSyncResp;
import cn.daxpay.open.channel.union.service.callback.UnionCallbackParseService;
import cn.daxpay.open.channel.union.service.close.UnionCloseService;
import cn.daxpay.open.channel.union.service.pay.UnionPayService;
import cn.daxpay.open.channel.union.service.refund.UnionRefundService;
import cn.daxpay.open.channel.union.service.refund.UnionRefundSyncService;
import cn.daxpay.open.channel.union.service.sync.UnionSyncService;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # 云闪付通道接收接口
///
/// 接收主应用 dax-pay-open 经声明式 HTTP 客户端转发的云闪付支付请求。
/// 通道与操作各自独立端点(通道前缀 `/channel/union`), 不做通用分发。
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channel/union")
public class UnionPayController {

    private final UnionPayService unionPayService;
    private final UnionCloseService unionCloseService;
    private final UnionRefundService unionRefundService;
    private final UnionSyncService unionSyncService;
    private final UnionRefundSyncService unionRefundSyncService;
    private final UnionCallbackParseService unionCallbackParseService;

    /// 支付下单
    @PostMapping("/pay")
    public DaxResult<UnionPayResp> pay(@Valid @RequestBody UnionPayReq req) {
        return DaxResult.ok(unionPayService.pay(req));
    }

    /// 关闭订单
    @PostMapping("/close")
    public DaxResult<UnionCloseResp> close(@Valid @RequestBody UnionCloseReq req) {
        return DaxResult.ok(unionCloseService.close(req));
    }

    /// 退款
    @PostMapping("/refund")
    public DaxResult<UnionRefundResp> refund(@Valid @RequestBody UnionRefundReq req) {
        return DaxResult.ok(unionRefundService.refund(req));
    }

    /// 支付同步(查询银联订单状态)
    @PostMapping("/sync")
    public DaxResult<UnionSyncResp> sync(@Valid @RequestBody UnionSyncReq req) {
        return DaxResult.ok(unionSyncService.sync(req));
    }

    /// 退款同步(查询退款状态)
    @PostMapping("/refund-sync")
    public DaxResult<UnionRefundSyncResp> refundSync(@Valid @RequestBody UnionRefundSyncReq req) {
        return DaxResult.ok(unionRefundSyncService.sync(req));
    }

    /// 支付回调验签解析(主应用转发)
    @PostMapping("/callback/parse-pay")
    public DaxResult<UnionCallbackParseResp> parsePayCallback(@RequestBody UnionCallbackParseReq req) {
        return DaxResult.ok(unionCallbackParseService.parsePay(req));
    }

    /// 退款回调验签解析(主应用转发)
    @PostMapping("/callback/parse-refund")
    public DaxResult<UnionCallbackParseResp> parseRefundCallback(@RequestBody UnionCallbackParseReq req) {
        return DaxResult.ok(unionCallbackParseService.parseRefund(req));
    }
}
