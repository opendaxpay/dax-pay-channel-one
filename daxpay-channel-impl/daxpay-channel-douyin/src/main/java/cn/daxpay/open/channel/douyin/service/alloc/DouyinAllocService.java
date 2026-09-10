package cn.daxpay.open.channel.douyin.service.alloc;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.req.DouyinAllocReq;
import cn.daxpay.open.channel.douyin.resp.DouyinAllocResp;
import cn.daxpay.open.channel.douyin.utils.DouyinDateUtil;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import com.douyinpay.api.splitfund.ApiSplitFundPaymentsService;
import com.douyinpay.api.splitfund.models.ApiQuerySplitFundRequest;
import com.douyinpay.api.splitfund.models.ApiQuerySplitFundResponse;
import com.douyinpay.api.splitfund.models.ApiSplitFundResponse;
import com.douyinpay.api.splitfund.models.ReceiverInfoDto;
import com.douyinpay.api.splitfund.models.ReceiverSplitResultDto;
import com.douyinpay.define.DomainName;
import com.douyinpay.exception.DouyinpayException;
import com.douyinpay.exception.ServiceException;
import com.douyinpay.util.GsonUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// # 抖音通道分账服务
///
/// 分账发起(splitFund)与查询(querySplitFund)经 SDK [ApiSplitFundPaymentsService] 调用。
/// 状态映射见主应用 [cn.daxpay.open.channel.douyin.service.payment.alloc.DouyinAllocService]。
/// 金额单位: 分(抖音分账金额单位即分, 无需转换)。
@Slf4j
@Service
public class DouyinAllocService {

    /// 发起分账(splitFund, unfreezeUnsplit=true 自动解冻剩余)
    public DouyinAllocResp alloc(DouyinAllocReq req) {
        ApiSplitFundPaymentsService service = new ApiSplitFundPaymentsService.Builder()
                .douyinpayClient(DouyinSdkConfig.buildClient(req.getCredential()))
                .build();
        // 抖音线上 API 要求 description 必填, 官方 SDK 缺该字段, 经扩展子类补齐(见 ApiSplitFundRequestExt)
        ApiSplitFundRequestExt apiReq = new ApiSplitFundRequestExt();
        apiReq.setDescription("订单分账");
        apiReq.setAppId(req.getCredential().getDouyinAppId());
        apiReq.setMerchantId(req.getCredential().getMchId());
        apiReq.setTradeNo(req.getTradeNo());
        apiReq.setOutTradeNo(req.getOutTradeNo());
        apiReq.setUnfreezeUnsplit(true);
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            apiReq.setNotifyUrl(req.getNotifyUrl());
        }
        // 接收方列表
        List<ReceiverInfoDto> receiverInfos = new ArrayList<>();
        if (Objects.nonNull(req.getReceiverInfoDtos())) {
            for (DouyinAllocReq.ReceiverInfo r : req.getReceiverInfoDtos()) {
                ReceiverInfoDto dto = new ReceiverInfoDto();
                dto.setType(r.getType());
                dto.setAccount(r.getAccount());
                dto.setName(r.getName());
                dto.setAmount(r.getAmount().intValue());
                // 抖音要求每个接收方的分账描述(receivers[].description)必填,
                // 缺失报 PARAM_ERROR("description is empty", 错误提示的 /description 未标明位于 receivers 内)
                dto.setDescription("订单分账");
                receiverInfos.add(dto);
            }
        }
        apiReq.setReceiverInfoDtos(receiverInfos);
        try {
            ApiSplitFundResponse response = service.splitFund(apiReq);
            DouyinAllocResp resp = new DouyinAllocResp();
            resp.setOrderId(response.getOrderId());
            return resp;
        } catch (DouyinpayException e) {
            log.error("抖音分账发起失败: allocNo={}", req.getOutTradeNo(), e);
            DouyinAllocResp resp = new DouyinAllocResp();
            fillError(e, resp);
            return resp;
        }
    }

    /// 分账同步查询(querySplitFund)
    public DouyinAllocResp sync(DouyinAllocReq req) {
        ApiSplitFundPaymentsService service = new ApiSplitFundPaymentsService.Builder()
                .douyinpayClient(DouyinSdkConfig.buildClient(req.getCredential()))
                .build();
        ApiQuerySplitFundRequest apiReq = new ApiQuerySplitFundRequest();
        apiReq.setMerchantId(req.getCredential().getMchId());
        apiReq.setTradeNo(req.getTradeNo());
        apiReq.setOutTradeNo(req.getOutTradeNo());
        try {
            ApiQuerySplitFundResponse response = service.querySplitFund(apiReq);
            DouyinAllocResp resp = new DouyinAllocResp();
            resp.setOrderId(response.getOrderId());
            resp.setStatus(response.getState());
            // 映射逐明细结果
            List<DouyinAllocResp.ReceiverSplitResult> results = new ArrayList<>();
            if (Objects.nonNull(response.getReceiverSplitResultDtos())) {
                for (ReceiverSplitResultDto r : response.getReceiverSplitResultDtos()) {
                    DouyinAllocResp.ReceiverSplitResult rr = new DouyinAllocResp.ReceiverSplitResult();
                    rr.setAccount(r.getAccount());
                    rr.setAmount(Objects.nonNull(r.getAmount()) ? r.getAmount().longValue() : null);
                    rr.setSplitStatus(r.getResult());
                    rr.setFailReason(r.getFailReason());
                    // 明细完成时间解析为 OffsetDateTime(无时区字面量按东八区), 与主应用镜像字段类型对齐
                    rr.setFinishTime(DouyinDateUtil.parse(r.getFinishTime()));
                    results.add(rr);
                }
            }
            resp.setReceiverSplitResultDtos(results);
            return resp;
        } catch (DouyinpayException e) {
            log.error("抖音分账查询失败: allocNo={}", req.getOutTradeNo(), e);
            DouyinAllocResp resp = new DouyinAllocResp();
            fillError(e, resp);
            return resp;
        }
    }

    /// 填充异常响应的错误码与错误文案
    ///
    /// [ServiceException] 已结构化解析出通道错误码与文案;
    /// 其他 [DouyinpayException](如签名/网络类) 无结构化信息, 退回原始 message。
    /// 完整调试串(含请求体)仅保留在日志中, 不透传给前端。
    private void fillError(DouyinpayException e, DouyinAllocResp resp) {
        if (e instanceof ServiceException se) {
            resp.setErrorCode(se.getErrorCode());
            resp.setErrorMsg(buildErrorMsg(se));
        } else {
            resp.setErrorCode(e.getMessage());
            resp.setErrorMsg(e.getMessage());
        }
    }

    /// 拼接简洁错误文案: 通道 message + detail.issue(如 "参数错误: description is empty")
    private String buildErrorMsg(ServiceException e) {
        String msg = StrUtil.blankToDefault(e.getErrorMessage(), e.getMessage());
        try {
            JsonObject body = GsonUtil.getGson().fromJson(e.getResponseBody(), JsonObject.class);
            if (Objects.nonNull(body) && body.has("detail") && body.get("detail").isJsonObject()) {
                JsonElement issue = body.getAsJsonObject("detail").get("issue");
                if (Objects.nonNull(issue) && !issue.isJsonNull() && StrUtil.isNotBlank(issue.getAsString())) {
                    msg = msg + ": " + issue.getAsString();
                }
            }
        } catch (Exception ignore) {
            // 响应体非预期 JSON 结构时保留原始 message
        }
        return msg;
    }
}
