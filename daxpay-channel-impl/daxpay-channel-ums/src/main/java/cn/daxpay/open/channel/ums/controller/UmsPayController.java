package cn.daxpay.open.channel.ums.controller;

import cn.daxpay.open.channel.ums.req.UmsCallbackParseReq;
import cn.daxpay.open.channel.ums.req.UmsCloseReq;
import cn.daxpay.open.channel.ums.req.UmsPayReq;
import cn.daxpay.open.channel.ums.req.UmsRefundReq;
import cn.daxpay.open.channel.ums.req.UmsRefundSyncReq;
import cn.daxpay.open.channel.ums.req.UmsSyncReq;
import cn.daxpay.open.channel.ums.resp.UmsCallbackParseResp;
import cn.daxpay.open.channel.ums.resp.UmsCloseResp;
import cn.daxpay.open.channel.ums.resp.UmsPayResp;
import cn.daxpay.open.channel.ums.resp.UmsRefundResp;
import cn.daxpay.open.channel.ums.resp.UmsRefundSyncResp;
import cn.daxpay.open.channel.ums.resp.UmsSyncResp;
import cn.daxpay.open.channel.ums.service.callback.UmsCallbackParseService;
import cn.daxpay.open.channel.ums.service.close.UmsCloseService;
import cn.daxpay.open.channel.ums.service.pay.UmsPayService;
import cn.daxpay.open.channel.ums.service.refund.UmsRefundService;
import cn.daxpay.open.channel.ums.service.refund.UmsRefundSyncService;
import cn.daxpay.open.channel.ums.service.sync.UmsSyncService;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # 银联商务通道接收接口
///
/// 接收主应用 dax-pay-open 经声明式 HTTP 客户端转发的银联商务支付请求。
/// 通道与操作各自独立端点(通道前缀 `/channel/ums`), 不做通用分发。
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channel/ums")
public class UmsPayController {

    private final UmsPayService umsPayService;
    private final UmsCloseService umsCloseService;
    private final UmsRefundService umsRefundService;
    private final UmsSyncService umsSyncService;
    private final UmsRefundSyncService umsRefundSyncService;
    private final UmsCallbackParseService umsCallbackParseService;

    /// 支付下单
    @PostMapping("/pay")
    public DaxResult<UmsPayResp> pay(@Valid @RequestBody UmsPayReq req) {
        return DaxResult.ok(umsPayService.pay(req));
    }

    /// 关闭订单
    @PostMapping("/close")
    public DaxResult<UmsCloseResp> close(@Valid @RequestBody UmsCloseReq req) {
        return DaxResult.ok(umsCloseService.close(req));
    }

    /// 退款
    @PostMapping("/refund")
    public DaxResult<UmsRefundResp> refund(@Valid @RequestBody UmsRefundReq req) {
        return DaxResult.ok(umsRefundService.refund(req));
    }

    /// 支付同步(查询银联商务订单状态)
    @PostMapping("/sync")
    public DaxResult<UmsSyncResp> sync(@Valid @RequestBody UmsSyncReq req) {
        return DaxResult.ok(umsSyncService.sync(req));
    }

    /// 退款同步(查询退款状态)
    @PostMapping("/refund-sync")
    public DaxResult<UmsRefundSyncResp> refundSync(@Valid @RequestBody UmsRefundSyncReq req) {
        return DaxResult.ok(umsRefundSyncService.sync(req));
    }

    /// 支付回调验签解析(主应用转发)
    @PostMapping("/callback/parse-pay")
    public DaxResult<UmsCallbackParseResp> parsePayCallback(@RequestBody UmsCallbackParseReq req) {
        return DaxResult.ok(umsCallbackParseService.parsePay(req));
    }

    /// 退款回调验签解析(主应用转发)
    @PostMapping("/callback/parse-refund")
    public DaxResult<UmsCallbackParseResp> parseRefundCallback(@RequestBody UmsCallbackParseReq req) {
        return DaxResult.ok(umsCallbackParseService.parseRefund(req));
    }
}
