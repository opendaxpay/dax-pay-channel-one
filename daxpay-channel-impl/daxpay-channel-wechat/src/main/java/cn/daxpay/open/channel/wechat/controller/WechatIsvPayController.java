package cn.daxpay.open.channel.wechat.controller;

import cn.daxpay.open.channel.wechat.req.WechatCloseReq;
import cn.daxpay.open.channel.wechat.req.WechatPayReq;
import cn.daxpay.open.channel.wechat.req.WechatRefundReq;
import cn.daxpay.open.channel.wechat.req.WechatRefundSyncReq;
import cn.daxpay.open.channel.wechat.req.WechatSyncReq;
import cn.daxpay.open.channel.wechat.resp.WechatCloseResp;
import cn.daxpay.open.channel.wechat.resp.WechatPayResp;
import cn.daxpay.open.channel.wechat.resp.WechatRefundResp;
import cn.daxpay.open.channel.wechat.resp.WechatRefundSyncResp;
import cn.daxpay.open.channel.wechat.resp.WechatSyncResp;
import cn.daxpay.open.channel.wechat.service.isv.WechatIsvCloseService;
import cn.daxpay.open.channel.wechat.service.isv.WechatIsvPayService;
import cn.daxpay.open.channel.wechat.service.isv.WechatIsvRefundService;
import cn.daxpay.open.channel.wechat.service.isv.WechatIsvRefundSyncService;
import cn.daxpay.open.channel.wechat.service.isv.WechatIsvSyncService;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # 微信服务商通道接收接口
///
/// 接收主应用 dax-pay-open 经声明式 HTTP 客户端转发的微信服务商支付请求。
/// 内部委托 [cn.daxpay.open.channel.wechat.service.isv.*] 调用微信 V3 服务商接口(`/v3/partner/transactions/*`)。
/// 与直连接口([WechatDirectPayController])端点结构一致, 路径前缀 `/channel/wechat/isv`。
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channel/wechat/isv")
public class WechatIsvPayController {

    private final WechatIsvPayService wechatIsvPayService;
    private final WechatIsvSyncService wechatIsvSyncService;
    private final WechatIsvCloseService wechatIsvCloseService;
    private final WechatIsvRefundService wechatIsvRefundService;
    private final WechatIsvRefundSyncService wechatIsvRefundSyncService;

    /// 支付下单
    @PostMapping("/pay")
    public DaxResult<WechatPayResp> pay(@Valid @RequestBody WechatPayReq req) {
        return DaxResult.ok(wechatIsvPayService.pay(req));
    }

    /// 支付同步(查询微信订单状态)
    @PostMapping("/sync")
    public DaxResult<WechatSyncResp> sync(@Valid @RequestBody WechatSyncReq req) {
        return DaxResult.ok(wechatIsvSyncService.sync(req));
    }

    /// 关闭微信订单
    @PostMapping("/close")
    public DaxResult<WechatCloseResp> close(@Valid @RequestBody WechatCloseReq req) {
        return DaxResult.ok(wechatIsvCloseService.close(req));
    }

    /// 退款
    @PostMapping("/refund")
    public DaxResult<WechatRefundResp> refund(@Valid @RequestBody WechatRefundReq req) {
        return DaxResult.ok(wechatIsvRefundService.refund(req));
    }

    /// 退款同步(查询退款状态)
    @PostMapping("/refund-sync")
    public DaxResult<WechatRefundSyncResp> refundSync(@Valid @RequestBody WechatRefundSyncReq req) {
        return DaxResult.ok(wechatIsvRefundSyncService.sync(req));
    }
}
