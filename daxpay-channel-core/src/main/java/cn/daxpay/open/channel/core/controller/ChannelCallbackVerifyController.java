package cn.daxpay.open.channel.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import cn.daxpay.open.channel.common.dto.callback.ChannelCallbackVerifyReq;
import cn.daxpay.open.channel.common.dto.callback.ChannelCallbackVerifyResp;
import cn.daxpay.open.channel.core.result.DaxResult;
import cn.daxpay.open.channel.core.service.ChannelRouterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelCallbackVerifyController {

    private final ChannelRouterService routerService;

    @PostMapping("/callback/verify")
    public DaxResult<ChannelCallbackVerifyResp> verify(@Valid @RequestBody ChannelCallbackVerifyReq req) {
        ChannelCallbackVerifyResp resp = routerService.getCallbackVerifyService(req.getChannel()).verify(req);
        return DaxResult.ok(resp);
    }
}
