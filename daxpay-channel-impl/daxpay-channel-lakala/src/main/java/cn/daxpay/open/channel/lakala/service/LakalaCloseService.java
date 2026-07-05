package cn.daxpay.open.channel.lakala.service;

import cn.daxpay.open.channel.lakala.code.LakalaCode;
import cn.daxpay.open.channel.lakala.req.LakalaCloseReq;
import cn.daxpay.open.channel.lakala.resp.LakalaCloseResp;
import cn.daxpay.open.channel.lakala.sdk.LakalaClient;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/// # 拉卡拉通道关单服务
///
/// 走 `/v3/labs/relation/close`。关单仅对未完成的扫码支付(主扫)有效。
@Slf4j
@Service
public class LakalaCloseService {

    /// 关闭订单
    public LakalaCloseResp close(LakalaCloseReq req) {
        log.info("拉卡拉关单请求: originOutTradeNo={}, originTradeNo={}",
                req.getOriginOutTradeNo(), req.getOriginTradeNo());
        var credential = req.getCredential();
        Map<String, Object> bizParam = new LinkedHashMap<>();
        bizParam.put("merchant_no", credential.getLakalaMchNo());
        bizParam.put("term_no", credential.getTermNo());
        if (StrUtil.isNotBlank(req.getOriginOutTradeNo())) {
            bizParam.put("origin_out_trade_no", req.getOriginOutTradeNo());
        }
        if (StrUtil.isNotBlank(req.getOriginTradeNo())) {
            bizParam.put("origin_trade_no", req.getOriginTradeNo());
        }
        Map<String, Object> locationInfo = new LinkedHashMap<>();
        locationInfo.put("request_ip", req.getClientIp());
        bizParam.put("location_info", locationInfo);
        try {
            LakalaClient.tradePost(credential, bizParam, LakalaCode.PATH_CLOSE);
            return new LakalaCloseResp();
        } catch (ChannelServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("拉卡拉关单失败: originOutTradeNo={}", req.getOriginOutTradeNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.lakalaCloseFailed", e.getMessage());
        }
    }
}
