package cn.daxpay.open.channel.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import cn.daxpay.open.channel.common.dto.refund.ChannelRefundReq;
import cn.daxpay.open.channel.common.dto.refund.ChannelRefundResp;
import cn.daxpay.open.channel.core.cache.service.ChannelInvokeCacheService;
import cn.daxpay.open.channel.core.result.DaxResult;
import cn.daxpay.open.channel.core.service.ChannelRefundService;
import cn.daxpay.open.channel.core.service.ChannelRouterService;
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
