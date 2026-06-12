package org.dromara.daxpay.channel.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.daxpay.channel.common.dto.close.ChannelCloseReq;
import org.dromara.daxpay.channel.common.dto.close.ChannelCloseResp;
import org.dromara.daxpay.channel.core.cache.service.ChannelInvokeCacheService;
import org.dromara.daxpay.channel.core.result.DaxResult;
import org.dromara.daxpay.channel.core.service.ChannelCloseService;
import org.dromara.daxpay.channel.core.service.ChannelRouterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelCloseController {

    private final ChannelRouterService routerService;
    private final ChannelInvokeCacheService cacheService;
    private final ObjectMapper objectMapper;

    @PostMapping("/close")
    public DaxResult<ChannelCloseResp> close(@Valid @RequestBody ChannelCloseReq req) throws Exception {
        var cached = cacheService.getCachedResponse(req.getChannel(), "close", req.getBizOrderNo());
        if (cached.isPresent()) {
            return DaxResult.ok(objectMapper.readValue(cached.get(), ChannelCloseResp.class));
        }
        boolean acquired = cacheService.tryAcquire(req.getChannel(), "close", req.getBizOrderNo(), objectMapper.writeValueAsString(req));
        if (!acquired) {
            ChannelCloseResp processingResp = new ChannelCloseResp();
            processingResp.setBizOrderNo(req.getBizOrderNo());
            return DaxResult.ok(processingResp);
        }
        try {
            ChannelCloseService closeService = routerService.getCloseService(req.getChannel());
            ChannelCloseResp resp = closeService.close(req);
            cacheService.setSuccess(req.getChannel(), "close", req.getBizOrderNo(), objectMapper.writeValueAsString(resp));
            return DaxResult.ok(resp);
        } catch (Exception e) {
            cacheService.setFail(req.getChannel(), "close", req.getBizOrderNo(), e.getMessage());
            throw e;
        }
    }
}
