package cn.daxpay.open.channel.lakala.service;

import cn.daxpay.open.channel.lakala.code.LakalaCode;
import cn.daxpay.open.channel.lakala.config.LakalaSdkCredential;
import cn.daxpay.open.channel.lakala.enums.LakalaPayBodyType;
import cn.daxpay.open.channel.lakala.enums.LakalaPayMethod;
import cn.daxpay.open.channel.lakala.req.LakalaPayReq;
import cn.daxpay.open.channel.lakala.resp.LakalaPayResp;
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

/// # 拉卡拉通道支付服务
///
/// 按 [LakalaPayReq.method] 分发到对应支付方式:
/// - MICROPAY(条码): 走 `/v3/labs/trans/micropay`, accountType 由拉卡拉据 authCode 自动识别
/// - PREORDER(预下单): 走 `/v3/labs/trans/preorder`, 按 accountType + transType 决定底层渠道(微信/支付宝/银联)
///
/// 金额: 平台内部单位为「分」(Long), 拉卡拉接口也是「分」(整数型字符), 直接透传。
@Slf4j
@Service
public class LakalaPayService {

    /// 拉卡拉时间格式(yyyyMMddHHmmss)
    private static final DateTimeFormatter LKL_TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.PURE_DATETIME_PATTERN);

    /// 支付下单
    public LakalaPayResp pay(LakalaPayReq req) {
        log.info("拉卡拉支付请求: outTradeNo={}, amount={}, method={}, accountType={}, transType={}",
                req.getOutTradeNo(), req.getAmount(), req.getMethod(), req.getAccountType(), req.getTransType());
        LakalaPayResp resp = new LakalaPayResp();
        resp.setOutTradeNo(req.getOutTradeNo());
        resp.setComplete(false);
        try {
            if (req.getMethod() == LakalaPayMethod.MICROPAY) {
                doMicropay(req, resp);
            } else {
                doPreorder(req, resp);
            }
        } catch (ChannelServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("拉卡拉支付调用失败: outTradeNo={}", req.getOutTradeNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.lakalaPayFailed", e.getMessage());
        }
        return resp;
    }

    /// 条码支付(/v3/labs/trans/micropay)
    ///
    /// 同步返回扣款结果: need_query=0 表示扣款成功, 其他表示处理中(需主应用轮询同步)。
    private void doMicropay(LakalaPayReq req, LakalaPayResp resp) {
        if (StrUtil.isBlank(req.getAuthCode())) {
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS, "channel.error.validateParams");
        }
        LakalaSdkCredential credential = req.getCredential();
        Map<String, Object> bizParam = buildBaseBizParam(req, credential);
        bizParam.put("auth_code", req.getAuthCode());
        // 条码支付无法预判渠道(拉卡拉据 authCode 自动识别), 支付宝收单需要 store_id
        // 传 true 无条件上送, 微信/银联会忽略该字段
        applyAccBusiFields(bizParam, req, credential, true);
        JSONObject respData = LakalaClient.tradePost(credential, bizParam, LakalaCode.PATH_MICROPAY);
        parseMicropayResp(respData, resp);
    }

    /// 预下单(/v3/labs/trans/preorder)
    private void doPreorder(LakalaPayReq req, LakalaPayResp resp) {
        if (StrUtil.isBlank(req.getAccountType()) || StrUtil.isBlank(req.getTransType())) {
            throw new ChannelServiceException(ChannelErrorCode.VALIDATE_PARAMS, "channel.error.validateParams");
        }
        LakalaSdkCredential credential = req.getCredential();
        Map<String, Object> bizParam = buildBaseBizParam(req, credential);
        bizParam.put("account_type", req.getAccountType());
        bizParam.put("trans_type", req.getTransType());
        // 账户业务扩展字段: 支付宝收单上送 store_id, JSAPI/MINI 上送 user_id
        applyAccBusiFields(bizParam, req, credential, "ALIPAY".equals(req.getAccountType()));
        JSONObject respData = LakalaClient.tradePost(credential, bizParam, LakalaCode.PATH_PREORDER);
        parsePreorderResp(respData, req, resp);
    }

    /// 构建基础业务参数(下单/条码通用)
    private Map<String, Object> buildBaseBizParam(LakalaPayReq req, LakalaSdkCredential credential) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("merchant_no", credential.getLakalaMchNo());
        param.put("term_no", credential.getTermNo());
        param.put("out_trade_no", req.getOutTradeNo());
        // 金额: 单位分, 拉卡拉要求整数型字符, 直接透传
        param.put("total_amount", String.valueOf(req.getAmount()));
        param.put("subject", req.getTitle());
        if (StrUtil.isNotBlank(req.getDescription())) {
            param.put("remark", req.getDescription());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            param.put("notify_url", req.getNotifyUrl());
        }
        // 地理位置信息
        Map<String, Object> locationInfo = new LinkedHashMap<>();
        locationInfo.put("request_ip", req.getClientIp());
        param.put("location_info", locationInfo);
        return param;
    }

    /// 构建账户业务扩展字段(acc_busi_fields)
    ///
    /// 按拉卡拉 V3 接口要求, 不同渠道上送不同扩展参数(对齐商业版 V3LabsTrade*AlipayBus 模型):
    /// - 支付宝收单(alipayScene=true): 上送 `store_id`(商户门店编号, 文档标注"支付宝收单上送", 条件必填 C)
    /// - JSAPI/MINI: 上送 `user_id`(微信 openId / 支付宝 buyerId)
    ///
    /// @param alipayScene 是否为支付宝收单场景
    ///                    (preorder 据 accountType 判断; micropay 无法预判渠道, 传 true 兜底, 微信/银联忽略 store_id)
    private void applyAccBusiFields(Map<String, Object> bizParam, LakalaPayReq req,
                                    LakalaSdkCredential credential, boolean alipayScene) {
        Map<String, Object> accBusiFields = null;
        // 商户门店编号(支付宝收单上送; TODO 门店对接后从门店配置读取)
        if (alipayScene && StrUtil.isNotBlank(credential.getStoreId())) {
            accBusiFields = new LinkedHashMap<>();
//            accBusiFields.put("store_id", credential.getStoreId());
        }
        // 用户标识(微信 openId / 支付宝 buyerId, JSAPI/MINI 场景必传)
        if (StrUtil.isNotBlank(req.getOpenId())) {
            if (accBusiFields == null) {
                accBusiFields = new LinkedHashMap<>();
            }
            accBusiFields.put("user_id", req.getOpenId());
        }
        if (accBusiFields != null) {
            bizParam.put("acc_busi_fields", accBusiFields);
        }
    }

    /// 解析条码支付响应
    private void parseMicropayResp(JSONObject respData, LakalaPayResp resp) {
        String tradeNo = respData.getStr("trade_no");
        resp.setTradeNo(tradeNo);
        String needQuery = respData.getStr("need_query");
        // need_query=0 表示扣款成功
        if ("0".equals(needQuery)) {
            resp.setComplete(true);
            // 实付金额(分)
            Integer payerAmount = respData.getInt("payer_amount");
            if (payerAmount != null) {
                resp.setPayerAmount(payerAmount.longValue());
                resp.setTotalAmount(payerAmount.longValue());
            }
            // 完成时间
            String tradeTime = respData.getStr("trade_time");
            if (StrUtil.isNotBlank(tradeTime)) {
                resp.setFinishTime(parseLakalaTime(tradeTime));
            }
            // 买家标识(account_type 决定字段名)
            String accountType = respData.getStr("account_type");
            JSONObject accResp = respData.getJSONObject("acc_resp_fields");
            if (accResp != null && StrUtil.isNotBlank(accountType)) {
                resp.setBuyerId(extractBuyerId(accountType, accResp));
            }
        }
    }

    /// 解析预下单响应(扫码返回二维码链接, JSAPI/APP/小程序返回调起参数)
    private void parsePreorderResp(JSONObject respData, LakalaPayReq req, LakalaPayResp resp) {
        String tradeNo = respData.getStr("trade_no");
        resp.setTradeNo(tradeNo);
        JSONObject accResp = respData.getJSONObject("acc_resp_fields");
        if (accResp == null) {
            return;
        }
        // 按 req 预设的 payBodyType 决定提取方式(主应用预先计算好)
        LakalaPayBodyType bodyType = req.getPayBodyType();
        if (bodyType == null) {
            // 兜底: 从 acc_resp_fields 内容推断
            bodyType = inferBodyType(req.getAccountType(), req.getTransType());
        }
        resp.setPayBodyType(bodyType);
        switch (bodyType) {
            case QR_CODE -> {
                // 扫码: code 字段(支付宝/银联)
                resp.setPayBody(accResp.getStr("code"));
            }
            case LINK -> {
                // 银联 JSAPI: redirect_url
                resp.setPayBody(accResp.getStr("redirect_url"));
            }
            case IDENTIFIER -> {
                // 支付宝 JSAPI: prepay_id
                resp.setPayBody(accResp.getStr("prepay_id"));
            }
            case JSAPI -> {
                // 微信 JSAPI/APP/小程序: acc_resp_fields 整体 JSON 作为调起参数
                resp.setPayBody(JSONUtil.toJsonStr(accResp));
            }
        }
    }

    /// 根据 accountType + transType 推断支付内容类型
    private LakalaPayBodyType inferBodyType(String accountType, String transType) {
        if ("WECHAT".equals(accountType)) {
            // 微信: 51/71 → JSAPI 调起参数
            return LakalaPayBodyType.JSAPI;
        }
        if ("ALIPAY".equals(accountType)) {
            if ("41".equals(transType)) {
                // 支付宝扫码
                return LakalaPayBodyType.QR_CODE;
            }
            // 支付宝 JSAPI/MINI
            return LakalaPayBodyType.IDENTIFIER;
        }
        if ("UQRCODEPAY".equals(accountType)) {
            if ("41".equals(transType)) {
                // 银联扫码
                return LakalaPayBodyType.QR_CODE;
            }
            // 银联 JSAPI
            return LakalaPayBodyType.LINK;
        }
        return LakalaPayBodyType.IDENTIFIER;
    }

    /// 提取买家标识(account_type 决定字段名)
    private String extractBuyerId(String accountType, JSONObject accResp) {
        if ("ALIPAY".equals(accountType) || "UQRCODEPAY".equals(accountType)) {
            return accResp.getStr("user_id");
        }
        if ("WECHAT".equals(accountType)) {
            return accResp.getStr("open_id");
        }
        return null;
    }

    /// 解析拉卡拉时间(yyyyMMddHHmmss → OffsetDateTime)
    private OffsetDateTime parseLakalaTime(String time) {
        try {
            return LocalDateTimeUtil.parse(time, LKL_TIME_FORMATTER).atOffset(OffsetDateTime.now().getOffset());
        } catch (Exception e) {
            log.warn("拉卡拉时间解析失败: time={}", time);
            return null;
        }
    }
}
