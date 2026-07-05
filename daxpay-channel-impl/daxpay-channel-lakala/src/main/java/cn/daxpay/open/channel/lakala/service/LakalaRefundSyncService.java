package cn.daxpay.open.channel.lakala.service;

import cn.daxpay.open.channel.lakala.code.LakalaCode;
import cn.daxpay.open.channel.lakala.req.LakalaRefundSyncReq;
import cn.daxpay.open.channel.lakala.resp.LakalaRefundSyncResp;
import cn.daxpay.open.channel.lakala.sdk.LakalaClient;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/// # 拉卡拉通道退款同步服务
///
/// 走 `/v3/labs/query/tradequery` 查询退款最终状态(退款单作为一笔交易, out_trade_no 用退款号)。
@Slf4j
@Service
public class LakalaRefundSyncService {

    private static final DateTimeFormatter LKL_TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.PURE_DATETIME_PATTERN);

    /// 查询退款状态
    public LakalaRefundSyncResp sync(LakalaRefundSyncReq req) {
        log.info("拉卡拉退款同步: outRefundNo={}, originTradeNo={}", req.getOutRefundNo(), req.getOriginTradeNo());
        var credential = req.getCredential();
        Map<String, Object> bizParam = new LinkedHashMap<>();
        bizParam.put("merchant_no", credential.getLakalaMchNo());
        bizParam.put("term_no", credential.getTermNo());
        bizParam.put("lkl_app_id", credential.getLklAppId());
        if (StrUtil.isNotBlank(req.getOutRefundNo())) {
            bizParam.put("out_trade_no", req.getOutRefundNo());
        }
        if (StrUtil.isNotBlank(req.getOriginTradeNo())) {
            bizParam.put("trade_no", req.getOriginTradeNo());
        }
        try {
            JSONObject respData = LakalaClient.tradePost(credential, bizParam, LakalaCode.PATH_QUERY);
            LakalaRefundSyncResp resp = new LakalaRefundSyncResp();
            resp.setSyncData(JSONUtil.toJsonStr(respData));
            resp.setOutRefundNo(respData.getStr("out_trade_no"));
            resp.setTradeNo(respData.getStr("trade_no"));
            String tradeState = respData.getStr("trade_state");
            resp.setRefundStatus(tradeState);
            String tradeTime = respData.getStr("trade_time");
            if (StrUtil.isNotBlank(tradeTime)) {
                resp.setFinishTime(parseLakalaTime(tradeTime));
            }
            return resp;
        } catch (ChannelServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("拉卡拉退款同步失败: outRefundNo={}", req.getOutRefundNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.lakalaSyncFailed", e.getMessage());
        }
    }

    private OffsetDateTime parseLakalaTime(String time) {
        try {
            return LocalDateTimeUtil.parse(time, LKL_TIME_FORMATTER).atOffset(OffsetDateTime.now().getOffset());
        } catch (Exception e) {
            return null;
        }
    }
}
