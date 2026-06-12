package org.dromara.daxpay.channel.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.daxpay.channel.common.dto.refund.ChannelRefundReq;
import org.dromara.daxpay.channel.common.dto.refund.ChannelRefundResp;
import org.dromara.daxpay.channel.core.cache.service.ChannelInvokeCacheService;
import org.dromara.daxpay.channel.core.result.DaxResult;
import org.dromara.daxpay.channel.core.service.ChannelRefundService;
import org.dromara.daxpay.channel.core.service.ChannelRouterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelRefundController {

    private final ChannelRouterService routerService;
    private final ChannelInvokeCacheService cacheService;
    private final ObjectMapper objectMapper;

    @PostMapping("/refund")
    public DaxResult<ChannelRefundResp> refund(@Valid @RequestBody ChannelRefundReq req) throws Exception {
        var cached = cacheService.getCachedResponse(req.getChannel(), "refund", req.getBizRefundOrderNo());
        if (cached.isPresent()) {
            return DaxResult.ok(objectMapper.readValue(cached.get(), ChannelRefundResp.class));
        }
        boolean acquired = cacheService.tryAcquire(req.getChannel(), "refund", req.getBizRefundOrderNo(), objectMapper.writeValueAsString(req));
        if (!acquired) {
            ChannelRefundResp processingResp = new ChannelRefundResp();
            processingResp.setBizRefundOrderNo(req.getBizRefundOrderNo());
            return DaxResult.ok(processingResp);
        }
        try {
            ChannelRefundService refundService = routerService.getRefundService(req.getChannel());
            ChannelRefundResp resp = refundService.refund(req);
            cacheService.setSuccess(req.getChannel(), "refund", req.getBizRefundOrderNo(), objectMapper.writeValueAsString(resp));
            return DaxResult.ok(resp);
        } catch (Exception e) {
            cacheService.setFail(req.getChannel(), "refund", req.getBizRefundOrderNo(), e.getMessage());
            throw e;
        }
    }
}
