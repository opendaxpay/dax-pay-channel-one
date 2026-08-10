package cn.daxpay.open.channel.douyin.service.alloc;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.req.DouyinAllocReq;
import cn.daxpay.open.channel.douyin.resp.DouyinAllocResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import com.douyinpay.api.splitfund.ApiSplitFundPaymentsService;
import com.douyinpay.api.splitfund.models.ApiQuerySplitFundRequest;
import com.douyinpay.api.splitfund.models.ApiQuerySplitFundResponse;
import com.douyinpay.api.splitfund.models.ApiSplitFundRequest;
import com.douyinpay.api.splitfund.models.ApiSplitFundResponse;
import com.douyinpay.api.splitfund.models.ReceiverInfoDto;
import com.douyinpay.api.splitfund.models.ReceiverSplitResultDto;
import com.douyinpay.define.DomainName;
import com.douyinpay.exception.DouyinpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        ApiSplitFundRequest apiReq = new ApiSplitFundRequest();
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
        if (req.getReceiverInfoDtos() != null) {
            for (DouyinAllocReq.ReceiverInfo r : req.getReceiverInfoDtos()) {
                ReceiverInfoDto dto = new ReceiverInfoDto();
                dto.setType(r.getType());
                dto.setAccount(r.getAccount());
                dto.setName(r.getName());
                dto.setAmount(r.getAmount().intValue());
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
            resp.setErrorCode(e.getMessage());
            resp.setErrorMsg(e.getMessage());
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
            if (response.getReceiverSplitResultDtos() != null) {
                for (ReceiverSplitResultDto r : response.getReceiverSplitResultDtos()) {
                    DouyinAllocResp.ReceiverSplitResult rr = new DouyinAllocResp.ReceiverSplitResult();
                    rr.setAccount(r.getAccount());
                    rr.setAmount(r.getAmount() != null ? r.getAmount().longValue() : null);
                    rr.setSplitStatus(r.getResult());
                    rr.setFailReason(r.getFailReason());
                    rr.setFinishTime(r.getFinishTime());
                    results.add(rr);
                }
            }
            resp.setReceiverSplitResultDtos(results);
            return resp;
        } catch (DouyinpayException e) {
            log.error("抖音分账查询失败: allocNo={}", req.getOutTradeNo(), e);
            DouyinAllocResp resp = new DouyinAllocResp();
            resp.setErrorCode(e.getMessage());
            resp.setErrorMsg(e.getMessage());
            return resp;
        }
    }
}
