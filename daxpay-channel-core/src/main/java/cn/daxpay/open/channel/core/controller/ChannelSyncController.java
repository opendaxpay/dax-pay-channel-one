package cn.daxpay.open.channel.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import cn.daxpay.open.channel.common.dto.sync.ChannelSyncReq;
import cn.daxpay.open.channel.common.dto.sync.ChannelSyncResp;
import cn.daxpay.open.channel.core.result.DaxResult;
import cn.daxpay.open.channel.core.service.ChannelRouterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelSyncController {

    private final ChannelRouterService routerService;

    @PostMapping("/sync")
    public DaxResult<ChannelSyncResp> sync(@Valid @RequestBody ChannelSyncReq req) {
        ChannelSyncResp resp = routerService.getSyncService(req.getChannel()).sync(req);
        return DaxResult.ok(resp);
    }
}
