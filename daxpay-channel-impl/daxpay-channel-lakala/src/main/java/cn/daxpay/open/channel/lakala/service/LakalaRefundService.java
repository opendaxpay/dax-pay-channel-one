package cn.daxpay.open.channel.lakala.service;

import cn.daxpay.open.channel.lakala.code.LakalaCode;
import cn.daxpay.open.channel.lakala.req.LakalaRefundReq;
import cn.daxpay.open.channel.lakala.resp.LakalaRefundResp;
import cn.daxpay.open.channel.lakala.sdk.LakalaClient;
import cn.daxpay.open.platform.common.util.PayUtil;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/// # 拉卡拉通道退款服务
///
/// 原路退款, 支持部分退款。走 `/v3/labs/relation/refund`。
@Slf4j
@Service
public class LakalaRefundService {

    private static final DateTimeFormatter LKL_TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.PURE_DATETIME_PATTERN);

    /// 退款
    public LakalaRefundResp refund(LakalaRefundReq req) {
        log.info("拉卡拉退款请求: outRefundNo={}, originOutTradeNo={}, amount={}",
                req.getOutRefundNo(), req.getOriginOutTradeNo(), req.getAmount());
        var credential = req.getCredential();
        Map<String, Object> bizParam = new LinkedHashMap<>();
        bizParam.put("merchant_no", credential.getLakalaMchNo());
        bizParam.put("term_no", credential.getTermNo());
        bizParam.put("lkl_app_id", credential.getLklAppId());
        bizParam.put("out_trade_no", req.getOutRefundNo());
        bizParam.put("origin_trade_no", StrUtil.isNotBlank(req.getOriginTradeNo())
                ? req.getOriginTradeNo() : req.getOriginOutTradeNo());
        // 退款金额: 分 → 元
        bizParam.put("refund_amount", PayUtil.conversionFenToYuan(req.getAmount()).toPlainString());
        if (StrUtil.isNotBlank(req.getReason())) {
            bizParam.put("refund_reason", req.getReason());
        }
        // 地理位置信息
        Map<String, Object> locationInfo = new LinkedHashMap<>();
        locationInfo.put("request_ip", req.getClientIp());
        bizParam.put("location_info", locationInfo);

        try {
            JSONObject respData = LakalaClient.tradePost(credential, bizParam, LakalaCode.PATH_REFUND);
            LakalaRefundResp resp = new LakalaRefundResp();
            resp.setOutRefundNo(req.getOutRefundNo());
            resp.setTradeNo(respData.getStr("trade_no"));
            resp.setComplete(false);
            String tradeTime = respData.getStr("trade_time");
            if (StrUtil.isNotBlank(tradeTime)) {
                resp.setFinishTime(parseLakalaTime(tradeTime));
                resp.setComplete(true);
            }
            return resp;
        } catch (ChannelServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("拉卡拉退款调用失败: outRefundNo={}", req.getOutRefundNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.lakalaRefundFailed", e.getMessage());
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
