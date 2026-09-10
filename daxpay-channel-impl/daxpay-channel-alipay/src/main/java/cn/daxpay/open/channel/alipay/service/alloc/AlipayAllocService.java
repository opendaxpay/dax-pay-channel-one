package cn.daxpay.open.channel.alipay.service.alloc;

import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.alipay.req.AlipayAllocReq;
import cn.daxpay.open.channel.alipay.resp.AlipayAllocResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.OpenApiRoyaltyDetailInfoPojo;
import com.alipay.api.request.AlipayTradeOrderSettleQueryRequest;
import com.alipay.api.request.AlipayTradeOrderSettleRequest;
import com.alipay.api.response.AlipayTradeOrderSettleQueryResponse;
import com.alipay.api.response.AlipayTradeOrderSettleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/// # 支付宝通道分账服务
///
/// 分账发起(alipay.trade.order.settle)与分账查询(alipay.trade.order.settle.query)。
/// 状态映射见主应用 [cn.daxpay.open.channel.alipay.service.payment.alloc.AlipayAllocService]。
/// 金额单位: 分 → 元字符串。
@Slf4j
@Service
public class AlipayAllocService {

    /// 异步分账模式
    private static final String ROYALTY_MODE_ASYNC = "async";

    /// 支付宝时间格式(东八区)
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /// 发起分账(alipay.trade.order.settle)
    public AlipayAllocResp alloc(AlipayAllocReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        JSONObject bizContent = new JSONObject();
        bizContent.set("out_request_no", req.getOutRequestNo());
        bizContent.set("trade_no", req.getTradeNo());
        bizContent.set("royalty_mode", StrUtil.blankToDefault(req.getRoyaltyMode(), ROYALTY_MODE_ASYNC));
        // 分账子参数
        JSONArray royaltyParams = new JSONArray();
        if (Objects.nonNull(req.getRoyaltyParameters())) {
            for (AlipayAllocReq.RoyaltyParam rp : req.getRoyaltyParameters()) {
                JSONObject item = new JSONObject();
                item.set("trans_in", rp.getTransIn());
                if (StrUtil.isNotBlank(rp.getTransInType())) {
                    item.set("trans_in_type", rp.getTransInType());
                }
                item.set("amount", fenToYuan(rp.getAmount()));
                royaltyParams.add(item);
            }
        }
        bizContent.set("royalty_parameters", royaltyParams);

        AlipayTradeOrderSettleRequest request = new AlipayTradeOrderSettleRequest();
        request.setBizContent(JSONUtil.toJsonStr(bizContent));
        try {
            AlipayTradeOrderSettleResponse response =
                    AlipaySdkConfig.execute(client, req.getCredential(), request);
            AlipayAllocResp resp = new AlipayAllocResp();
            resp.setCode(response.getCode());
            resp.setSubCode(response.getSubCode());
            resp.setSubMsg(response.getSubMsg());
            // 业务失败时业务字段均为 null, 直接返回让主应用决策
            if (!response.isSuccess()) {
                log.warn("支付宝分账发起业务失败: allocNo={}, code={}, subCode={}, subMsg={}",
                        req.getOutRequestNo(), response.getCode(), response.getSubCode(), response.getSubMsg());
                return resp;
            }
            resp.setSettleNo(response.getSettleNo());
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝分账调用失败: allocNo={}", req.getOutRequestNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayAllocFailed", e.getMessage());
        }
    }

    /// 分账同步查询(alipay.trade.order.settle.query)
    public AlipayAllocResp sync(AlipayAllocReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        JSONObject bizContent = new JSONObject();
        bizContent.set("out_request_no", req.getOutRequestNo());
        bizContent.set("trade_no", req.getTradeNo());

        AlipayTradeOrderSettleQueryRequest request = new AlipayTradeOrderSettleQueryRequest();
        request.setBizContent(JSONUtil.toJsonStr(bizContent));
        try {
            AlipayTradeOrderSettleQueryResponse response =
                    AlipaySdkConfig.execute(client, req.getCredential(), request);
            AlipayAllocResp resp = new AlipayAllocResp();
            resp.setCode(response.getCode());
            resp.setSubCode(response.getSubCode());
            resp.setSubMsg(response.getSubMsg());
            if (!response.isSuccess()) {
                log.warn("支付宝分账查询业务失败: allocNo={}, code={}, subCode={}, subMsg={}",
                        req.getOutRequestNo(), response.getCode(), response.getSubCode(), response.getSubMsg());
                return resp;
            }
            resp.setSettleNo(response.getOutRequestNo());
            // 映射逐明细结果(查询响应不返回 settleNo, outRequestNo 即平台 allocNo)
            List<AlipayAllocResp.RoyaltyDetailResult> detailResults = new ArrayList<>();
            if (Objects.nonNull(response.getRoyaltyDetailList())) {
                for (var r : response.getRoyaltyDetailList()) {
                    AlipayAllocResp.RoyaltyDetailResult dr = new AlipayAllocResp.RoyaltyDetailResult();
                    dr.setDetailId(r.getDetailId());
                    dr.setTransIn(r.getTransIn());
                    dr.setState(r.getState());
                    dr.setErrorDesc(r.getErrorDesc());
                    dr.setExecuteDt(formatDate(r.getExecuteDt()));
                    detailResults.add(dr);
                }
            }
            resp.setRoyaltyDetailList(detailResults);
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝分账查询调用失败: allocNo={}", req.getOutRequestNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayAllocQueryFailed", e.getMessage());
        }
    }

    /// 分 → 元字符串(支付宝金额单位为元)
    private String fenToYuan(Long amount) {
        if (Objects.isNull(amount)) {
            return "0";
        }
        return BigDecimal.valueOf(amount).movePointLeft(2)
                .setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    /// Date → 东八区时间字符串(yyyy-MM-dd HH:mm:ss)
    private String formatDate(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return date.toInstant()
                .atZone(ZoneId.of("Asia/Shanghai"))
                .toLocalDateTime()
                .format(DATE_FMT);
    }
}
