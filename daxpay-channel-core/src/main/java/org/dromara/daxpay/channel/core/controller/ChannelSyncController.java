package org.dromara.daxpay.channel.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.daxpay.channel.common.dto.sync.ChannelSyncReq;
import org.dromara.daxpay.channel.common.dto.sync.ChannelSyncResp;
import org.dromara.daxpay.channel.core.result.DaxResult;
import org.dromara.daxpay.channel.core.service.ChannelRouterService;
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
