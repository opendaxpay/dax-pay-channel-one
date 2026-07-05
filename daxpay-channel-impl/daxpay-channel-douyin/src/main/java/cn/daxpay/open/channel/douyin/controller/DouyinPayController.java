package cn.daxpay.open.channel.douyin.controller;

import cn.daxpay.open.channel.douyin.req.DouyinCallbackParseReq;
import cn.daxpay.open.channel.douyin.req.DouyinCloseReq;
import cn.daxpay.open.channel.douyin.req.DouyinPayReq;
import cn.daxpay.open.channel.douyin.req.DouyinRefundReq;
import cn.daxpay.open.channel.douyin.req.DouyinRefundSyncReq;
import cn.daxpay.open.channel.douyin.req.DouyinSyncReq;
import cn.daxpay.open.channel.douyin.resp.DouyinCallbackParseResp;
import cn.daxpay.open.channel.douyin.resp.DouyinCloseResp;
import cn.daxpay.open.channel.douyin.resp.DouyinPayResp;
import cn.daxpay.open.channel.douyin.resp.DouyinRefundResp;
import cn.daxpay.open.channel.douyin.resp.DouyinRefundSyncResp;
import cn.daxpay.open.channel.douyin.resp.DouyinSyncResp;
import cn.daxpay.open.channel.douyin.service.callback.DouyinCallbackParseService;
import cn.daxpay.open.channel.douyin.service.close.DouyinCloseService;
import cn.daxpay.open.channel.douyin.service.pay.DouyinPayService;
import cn.daxpay.open.channel.douyin.service.refund.DouyinRefundService;
import cn.daxpay.open.channel.douyin.service.refund.DouyinRefundServiceSync;
import cn.daxpay.open.channel.douyin.service.sync.DouyinSyncService;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # 抖音通道接收接口
///
/// 接收主应用 dax-pay-open 经声明式 HTTP 客户端转发的抖音支付请求。
/// 通道与操作各自独立端点(通道前缀 `/channel/douyin`), 不做通用分发。
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channel/douyin")
public class DouyinPayController {

    private final DouyinPayService douyinPayService;
    private final DouyinCloseService douyinCloseService;
    private final DouyinRefundService douyinRefundService;
    private final DouyinSyncService douyinSyncService;
    private final DouyinRefundServiceSync douyinRefundServiceSync;
    private final DouyinCallbackParseService douyinCallbackParseService;

    /// 支付下单
    @PostMapping("/pay")
    public DaxResult<DouyinPayResp> pay(@Valid @RequestBody DouyinPayReq req) {
        return DaxResult.ok(douyinPayService.pay(req));
    }

    /// 关闭订单
    @PostMapping("/close")
    public DaxResult<DouyinCloseResp> close(@Valid @RequestBody DouyinCloseReq req) {
        return DaxResult.ok(douyinCloseService.close(req));
    }

    /// 退款
    @PostMapping("/refund")
    public DaxResult<DouyinRefundResp> refund(@Valid @RequestBody DouyinRefundReq req) {
        return DaxResult.ok(douyinRefundService.refund(req));
    }

    /// 支付同步(查询抖音订单状态)
    @PostMapping("/sync")
    public DaxResult<DouyinSyncResp> sync(@Valid @RequestBody DouyinSyncReq req) {
        return DaxResult.ok(douyinSyncService.sync(req));
    }

    /// 退款同步(查询退款状态)
    @PostMapping("/refund-sync")
    public DaxResult<DouyinRefundSyncResp> refundSync(@Valid @RequestBody DouyinRefundSyncReq req) {
        return DaxResult.ok(douyinRefundServiceSync.sync(req));
    }

    /// 支付回调验签解析(主应用转发)
    @PostMapping("/callback/parse-pay")
    public DaxResult<DouyinCallbackParseResp> parsePayCallback(@RequestBody DouyinCallbackParseReq req) {
        return DaxResult.ok(douyinCallbackParseService.parsePay(req));
    }

    /// 退款回调验签解析(主应用转发)
    @PostMapping("/callback/parse-refund")
    public DaxResult<DouyinCallbackParseResp> parseRefundCallback(@RequestBody DouyinCallbackParseReq req) {
        return DaxResult.ok(douyinCallbackParseService.parseRefund(req));
    }
}
