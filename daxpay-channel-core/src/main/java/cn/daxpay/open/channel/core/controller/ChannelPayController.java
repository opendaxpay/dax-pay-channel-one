package cn.daxpay.open.channel.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import cn.daxpay.open.channel.common.dto.pay.ChannelPayReq;
import cn.daxpay.open.channel.common.dto.pay.ChannelPayResp;
import cn.daxpay.open.channel.core.cache.service.ChannelInvokeCacheService;
import cn.daxpay.open.channel.core.result.DaxResult;
import cn.daxpay.open.channel.core.service.ChannelPayService;
import cn.daxpay.open.channel.core.service.ChannelRouterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelPayController {

    private final ChannelRouterService routerService;
    private final ChannelInvokeCacheService cacheService;
    private final ObjectMapper objectMapper;

    @PostMapping("/pay")
    public DaxResult<ChannelPayResp> pay(@Valid @RequestBody ChannelPayReq req) throws Exception {
        var cached = cacheService.getCachedResponse(req.getChannel(), "pay", req.getBizOrderNo());
        if (cached.isPresent()) {
            return DaxResult.ok(objectMapper.readValue(cached.get(), ChannelPayResp.class));
        }
        boolean acquired = cacheService.tryAcquire(req.getChannel(), "pay", req.getBizOrderNo(), objectMapper.writeValueAsString(req));
        if (!acquired) {
            ChannelPayResp processingResp = new ChannelPayResp();
            processingResp.setBizOrderNo(req.getBizOrderNo());
            return DaxResult.ok(processingResp);
        }
        try {
            ChannelPayService payService = routerService.getPayService(req.getChannel());
            ChannelPayResp resp = payService.pay(req);
            cacheService.setSuccess(req.getChannel(), "pay", req.getBizOrderNo(), objectMapper.writeValueAsString(resp));
            return DaxResult.ok(resp);
        } catch (Exception e) {
            cacheService.setFail(req.getChannel(), "pay", req.getBizOrderNo(), e.getMessage());
            throw e;
        }
    }
}
