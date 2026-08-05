package cn.daxpay.open.channel.wechat.controller;

import cn.daxpay.open.channel.wechat.req.WechatCallbackParseReq;
import cn.daxpay.open.channel.wechat.req.WechatCloseReq;
import cn.daxpay.open.channel.wechat.req.WechatPayReq;
import cn.daxpay.open.channel.wechat.req.WechatRefundReq;
import cn.daxpay.open.channel.wechat.req.WechatRefundSyncReq;
import cn.daxpay.open.channel.wechat.req.WechatSyncReq;
import cn.daxpay.open.channel.wechat.req.WechatTransferReq;
import cn.daxpay.open.channel.wechat.resp.WechatCallbackParseResp;
import cn.daxpay.open.channel.wechat.resp.WechatCloseResp;
import cn.daxpay.open.channel.wechat.resp.WechatPayResp;
import cn.daxpay.open.channel.wechat.resp.WechatRefundResp;
import cn.daxpay.open.channel.wechat.resp.WechatRefundSyncResp;
import cn.daxpay.open.channel.wechat.resp.WechatSyncResp;
import cn.daxpay.open.channel.wechat.resp.WechatTransferResp;
import cn.daxpay.open.channel.wechat.service.callback.WechatCallbackParseService;
import cn.daxpay.open.channel.wechat.service.direct.WechatDirectCloseService;
import cn.daxpay.open.channel.wechat.service.direct.WechatDirectPayService;
import cn.daxpay.open.channel.wechat.service.direct.WechatDirectRefundService;
import cn.daxpay.open.channel.wechat.service.direct.WechatDirectRefundSyncService;
import cn.daxpay.open.channel.wechat.service.direct.WechatDirectSyncService;
import cn.daxpay.open.channel.wechat.service.direct.WechatDirectTransferService;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # 微信通道接收接口
///
/// 接收主应用 dax-pay-open 经声明式 HTTP 客户端转发的微信支付请求。
/// 通道与操作各自独立端点(通道前缀 `/channel/wechat`), 不做通用分发。
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channel/wechat")
public class WechatDirectPayController {

    private final WechatDirectPayService wechatPayService;
    private final WechatDirectSyncService wechatSyncService;
    private final WechatDirectCloseService wechatCloseService;
    private final WechatDirectRefundService wechatRefundService;
    private final WechatDirectRefundSyncService wechatRefundSyncService;
    private final WechatDirectTransferService wechatDirectTransferService;
    private final WechatCallbackParseService wechatCallbackParseService;

    /// 支付下单
    @PostMapping("/pay")
    public DaxResult<WechatPayResp> pay(@Valid @RequestBody WechatPayReq req) {
        return DaxResult.ok(wechatPayService.pay(req));
    }

    /// 支付同步(查询微信订单状态)
    @PostMapping("/sync")
    public DaxResult<WechatSyncResp> sync(@Valid @RequestBody WechatSyncReq req) {
        return DaxResult.ok(wechatSyncService.sync(req));
    }

    /// 关闭微信订单
    @PostMapping("/close")
    public DaxResult<WechatCloseResp> close(@Valid @RequestBody WechatCloseReq req) {
        return DaxResult.ok(wechatCloseService.close(req));
    }

    /// 退款
    @PostMapping("/refund")
    public DaxResult<WechatRefundResp> refund(@Valid @RequestBody WechatRefundReq req) {
        return DaxResult.ok(wechatRefundService.refund(req));
    }

    /// 退款同步(查询退款状态)
    @PostMapping("/refund-sync")
    public DaxResult<WechatRefundSyncResp> refundSync(@Valid @RequestBody WechatRefundSyncReq req) {
        return DaxResult.ok(wechatRefundSyncService.sync(req));
    }

    /// 转账(商家转账到零钱 V3)
    @PostMapping("/transfer")
    public DaxResult<WechatTransferResp> transfer(@Valid @RequestBody WechatTransferReq req) {
        return DaxResult.ok(wechatDirectTransferService.transfer(req));
    }

    /// 转账同步(查询转账状态)
    @PostMapping("/transfer-sync")
    public DaxResult<WechatTransferResp> transferSync(@Valid @RequestBody WechatTransferReq req) {
        return DaxResult.ok(wechatDirectTransferService.sync(req));
    }

    /// 支付回调验签解析(主应用转发)
    @PostMapping("/callback/parse-pay")
    public DaxResult<WechatCallbackParseResp> parsePayCallback(@RequestBody WechatCallbackParseReq req) {
        return DaxResult.ok(wechatCallbackParseService.parsePay(req));
    }

    /// 退款回调验签解析(主应用转发)
    @PostMapping("/callback/parse-refund")
    public DaxResult<WechatCallbackParseResp> parseRefundCallback(@RequestBody WechatCallbackParseReq req) {
        return DaxResult.ok(wechatCallbackParseService.parseRefund(req));
    }
}
