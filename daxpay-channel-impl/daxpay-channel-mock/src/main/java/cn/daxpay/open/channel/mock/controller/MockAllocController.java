package cn.daxpay.open.channel.mock.controller;

import cn.daxpay.open.channel.mock.req.MockAllocFinishReq;
import cn.daxpay.open.channel.mock.req.MockAllocReceiverReq;
import cn.daxpay.open.channel.mock.req.MockAllocReq;
import cn.daxpay.open.channel.mock.req.MockAllocSyncReq;
import cn.daxpay.open.channel.mock.resp.MockAllocResp;
import cn.daxpay.open.channel.mock.service.alloc.MockAllocFinishService;
import cn.daxpay.open.channel.mock.service.alloc.MockAllocReceiverBindService;
import cn.daxpay.open.channel.mock.service.alloc.MockAllocReceiverUnbindService;
import cn.daxpay.open.channel.mock.service.alloc.MockAllocService;
import cn.daxpay.open.channel.mock.service.alloc.MockAllocSyncService;
import cn.daxpay.open.platform.core.result.DaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// # Mock 通道分账接收接口
///
/// 端点路径与主应用 MockChannelClient 的 @PostExchange 声明逐一镜像;
/// 分账接收方绑定/解绑无业务数据(DaxResult&lt;Void&gt;)。
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channel/mock")
public class MockAllocController {

    private final MockAllocService mockAllocService;
    private final MockAllocFinishService mockAllocFinishService;
    private final MockAllocSyncService mockAllocSyncService;
    private final MockAllocReceiverBindService mockAllocReceiverBindService;
    private final MockAllocReceiverUnbindService mockAllocReceiverUnbindService;

    /// 发起分账
    @PostMapping("/alloc")
    public DaxResult<MockAllocResp> alloc(@Valid @RequestBody MockAllocReq req) {
        return DaxResult.ok(mockAllocService.alloc(req));
    }

    /// 发起分账完结
    @PostMapping("/alloc-finish")
    public DaxResult<MockAllocResp> allocFinish(@Valid @RequestBody MockAllocFinishReq req) {
        return DaxResult.ok(mockAllocFinishService.allocFinish(req));
    }

    /// 分账同步(查询 mock 分账单状态)
    @PostMapping("/alloc-sync")
    public DaxResult<MockAllocResp> allocSync(@Valid @RequestBody MockAllocSyncReq req) {
        return DaxResult.ok(mockAllocSyncService.allocSync(req));
    }

    /// 绑定分账接收方
    @PostMapping("/alloc-receiver/bind")
    public DaxResult<Void> allocReceiverBind(@Valid @RequestBody MockAllocReceiverReq req) {
        mockAllocReceiverBindService.bind(req);
        return DaxResult.ok(null);
    }

    /// 解绑分账接收方
    @PostMapping("/alloc-receiver/unbind")
    public DaxResult<Void> allocReceiverUnbind(@Valid @RequestBody MockAllocReceiverReq req) {
        mockAllocReceiverUnbindService.unbind(req);
        return DaxResult.ok(null);
    }
}
