package cn.daxpay.open.channel.lakala.service;

import cn.daxpay.open.channel.lakala.code.LakalaCode;
import cn.daxpay.open.channel.lakala.req.LakalaSyncReq;
import cn.daxpay.open.channel.lakala.resp.LakalaSyncResp;
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
import java.util.Objects;

/// # 拉卡拉通道订单同步服务
///
/// 走 `/v3/labs/query/tradequery` 查询支付订单最终状态。
/// trade_state: SUCCESS(成功) / FAIL(失败) / CLOSED(已关闭) / 其他(处理中)。
@Slf4j
@Service
public class LakalaSyncService {

    private static final DateTimeFormatter LKL_TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.PURE_DATETIME_PATTERN);

    /// 同步订单状态
    public LakalaSyncResp sync(LakalaSyncReq req) {
        log.info("拉卡拉订单同步: outTradeNo={}, tradeNo={}", req.getOutTradeNo(), req.getTradeNo());
        var credential = req.getCredential();
        Map<String, Object> bizParam = new LinkedHashMap<>();
        bizParam.put("merchant_no", credential.getLakalaMchNo());
        bizParam.put("term_no", credential.getTermNo());
        bizParam.put("lkl_app_id", credential.getLklAppId());
        if (StrUtil.isNotBlank(req.getOutTradeNo())) {
            bizParam.put("out_trade_no", req.getOutTradeNo());
        }
        if (StrUtil.isNotBlank(req.getTradeNo())) {
            bizParam.put("trade_no", req.getTradeNo());
        }
        try {
            JSONObject respData = LakalaClient.tradePost(credential, bizParam, LakalaCode.PATH_QUERY);
            LakalaSyncResp resp = new LakalaSyncResp();
            resp.setSyncData(JSONUtil.toJsonStr(respData));
            resp.setTradeNo(respData.getStr("trade_no"));
            resp.setOutTradeNo(respData.getStr("out_trade_no"));
            String tradeState = respData.getStr("trade_state");
            resp.setTradeState(tradeState);
            // 成功时解析金额/时间/买家
            if (Objects.equals(tradeState, "SUCCESS")) {
                String totalAmount = respData.getStr("total_amount");
                if (StrUtil.isNotBlank(totalAmount)) {
                    resp.setTotalAmount(Long.parseLong(totalAmount));
                }
                String tradeTime = respData.getStr("trade_time");
                if (StrUtil.isNotBlank(tradeTime)) {
                    resp.setFinishTime(parseLakalaTime(tradeTime));
                }
                resp.setBuyerId(respData.getStr("user_id2"));
            }
            return resp;
        } catch (ChannelServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("拉卡拉订单同步失败: outTradeNo={}", req.getOutTradeNo(), e);
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
