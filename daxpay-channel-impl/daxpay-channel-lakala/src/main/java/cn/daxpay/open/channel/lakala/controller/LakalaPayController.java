package cn.daxpay.open.channel.lakala.controller;

import cn.daxpay.open.channel.lakala.req.LakalaCloseReq;
import cn.daxpay.open.channel.lakala.req.LakalaPayReq;
import cn.daxpay.open.channel.lakala.req.LakalaRefundReq;
import cn.daxpay.open.channel.lakala.req.LakalaRefundSyncReq;
import cn.daxpay.open.channel.lakala.req.LakalaSyncReq;
import cn.daxpay.open.channel.lakala.resp.LakalaCloseResp;
import cn.daxpay.open.channel.lakala.resp.LakalaPayResp;
import cn.daxpay.open.channel.lakala.resp.LakalaRefundResp;
import cn.daxpay.open.channel.lakala.resp.LakalaRefundSyncResp;
import cn.daxpay.open.channel.lakala.resp.LakalaSyncResp;
import cn.daxpay.open.channel.lakala.service.LakalaCloseService;
import cn.daxpay.open.channel.lakala.service.LakalaPayService;
import cn.daxpay.open.channel.lakala.service.LakalaRefundService;
import cn.daxpay.open.channel.lakala.service.LakalaRefundSyncService;
import cn.daxpay.open.channel.lakala.service.LakalaSyncService;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # 拉卡拉通道接收接口
///
/// 接收主应用 dax-pay-open 经声明式 HTTP 客户端转发的拉卡拉支付请求。
/// 内部委托 [cn.daxpay.open.channel.lakala.service.*] 调用拉卡拉 V3 接口(`/v3/labs/trans/*`、`/v3/labs/relation/*`、`/v3/labs/query/*`)。
/// 拉卡拉为聚合服务商模式, 不区分直连/服务商, 路径前缀 `/channel/lakala`。
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channel/lakala")
public class LakalaPayController {

    private final LakalaPayService lakalaPayService;
    private final LakalaSyncService lakalaSyncService;
    private final LakalaCloseService lakalaCloseService;
    private final LakalaRefundService lakalaRefundService;
    private final LakalaRefundSyncService lakalaRefundSyncService;

    /// 支付下单
    @PostMapping("/pay")
    public DaxResult<LakalaPayResp> pay(@Valid @RequestBody LakalaPayReq req) {
        return DaxResult.ok(lakalaPayService.pay(req));
    }

    /// 支付同步(查询拉卡拉订单状态)
    @PostMapping("/sync")
    public DaxResult<LakalaSyncResp> sync(@Valid @RequestBody LakalaSyncReq req) {
        return DaxResult.ok(lakalaSyncService.sync(req));
    }

    /// 关闭订单
    @PostMapping("/close")
    public DaxResult<LakalaCloseResp> close(@Valid @RequestBody LakalaCloseReq req) {
        return DaxResult.ok(lakalaCloseService.close(req));
    }

    /// 退款
    @PostMapping("/refund")
    public DaxResult<LakalaRefundResp> refund(@Valid @RequestBody LakalaRefundReq req) {
        return DaxResult.ok(lakalaRefundService.refund(req));
    }

    /// 退款同步(查询退款状态)
    @PostMapping("/refund-sync")
    public DaxResult<LakalaRefundSyncResp> refundSync(@Valid @RequestBody LakalaRefundSyncReq req) {
        return DaxResult.ok(lakalaRefundSyncService.sync(req));
    }
}
