package cn.daxpay.open.channel.mock.controller;

import cn.daxpay.open.channel.mock.req.MockCallbackParseReq;
import cn.daxpay.open.channel.mock.req.MockCloseReq;
import cn.daxpay.open.channel.mock.req.MockPayReq;
import cn.daxpay.open.channel.mock.req.MockRefundReq;
import cn.daxpay.open.channel.mock.req.MockRefundSyncReq;
import cn.daxpay.open.channel.mock.req.MockSyncReq;
import cn.daxpay.open.channel.mock.resp.MockCallbackParseResp;
import cn.daxpay.open.channel.mock.resp.MockCloseResp;
import cn.daxpay.open.channel.mock.resp.MockPayResp;
import cn.daxpay.open.channel.mock.resp.MockRefundResp;
import cn.daxpay.open.channel.mock.resp.MockSyncResp;
import cn.daxpay.open.channel.mock.service.callback.MockPayCallbackParseService;
import cn.daxpay.open.channel.mock.service.callback.MockRefundCallbackParseService;
import cn.daxpay.open.channel.mock.service.close.MockCloseService;
import cn.daxpay.open.channel.mock.service.pay.MockPayService;
import cn.daxpay.open.channel.mock.service.refund.MockRefundService;
import cn.daxpay.open.channel.mock.service.refund.MockRefundSyncService;
import cn.daxpay.open.channel.mock.service.sync.MockSyncService;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # Mock 通道支付/退款接收接口
///
/// 接收主应用 mock 编排经声明式 HTTP 客户端(AES-GCM 传输加密)转发的支付族请求,
/// 端点路径与主应用 MockChannelClient 的 @PostExchange 声明逐一镜像;
/// HTTP 恒 200, 业务成败由 [DaxResult#code] 表达(one 既有风格)。
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channel/mock")
public class MockPayController {

    private final MockPayService mockPayService;
    private final MockSyncService mockSyncService;
    private final MockCloseService mockCloseService;
    private final MockRefundService mockRefundService;
    private final MockRefundSyncService mockRefundSyncService;
    private final MockPayCallbackParseService mockPayCallbackParseService;
    private final MockRefundCallbackParseService mockRefundCallbackParseService;

    /// 支付下单
    @PostMapping("/pay")
    public DaxResult<MockPayResp> pay(@Valid @RequestBody MockPayReq req) {
        return DaxResult.ok(mockPayService.pay(req));
    }

    /// 支付同步(查询 mock 订单状态)
    @PostMapping("/sync")
    public DaxResult<MockSyncResp> sync(@Valid @RequestBody MockSyncReq req) {
        return DaxResult.ok(mockSyncService.sync(req));
    }

    /// 关闭 mock 订单
    @PostMapping("/close")
    public DaxResult<MockCloseResp> close(@Valid @RequestBody MockCloseReq req) {
        return DaxResult.ok(mockCloseService.close(req));
    }

    /// 发起退款
    @PostMapping("/refund")
    public DaxResult<MockRefundResp> refund(@Valid @RequestBody MockRefundReq req) {
        return DaxResult.ok(mockRefundService.refund(req));
    }

    /// 退款同步(查询 mock 退款单状态)
    @PostMapping("/refund-sync")
    public DaxResult<MockRefundResp> refundSync(@Valid @RequestBody MockRefundSyncReq req) {
        return DaxResult.ok(mockRefundSyncService.sync(req));
    }

    /// 支付回调验签解析(主应用转发 body+签名三头原值)
    @PostMapping("/callback/parse-pay")
    public DaxResult<MockCallbackParseResp> parsePayCallback(@RequestBody MockCallbackParseReq req) {
        return DaxResult.ok(mockPayCallbackParseService.parse(req));
    }

    /// 退款回调验签解析(主应用转发 body+签名三头原值)
    @PostMapping("/callback/parse-refund")
    public DaxResult<MockCallbackParseResp> parseRefundCallback(@RequestBody MockCallbackParseReq req) {
        return DaxResult.ok(mockRefundCallbackParseService.parse(req));
    }
}
