package cn.daxpay.open.channel.douyin.service.refund;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.req.DouyinRefundReq;
import cn.daxpay.open.channel.douyin.resp.DouyinRefundResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import com.douyinpay.api.refund.model.ApiAmountReq;
import com.douyinpay.api.refund.model.ApiCreateRequest;
import com.douyinpay.exception.DouyinpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 抖音通道退款服务
@Slf4j
@Service
public class DouyinRefundService {

    /// 货币种类
    private static final String CURRENCY_CNY = "CNY";

    /// 发起抖音退款
    public DouyinRefundResp refund(DouyinRefundReq req) {
        ApiCreateRequest request = new ApiCreateRequest();
        request.setAppid(req.getCredential().getDouyinAppId());
        request.setMchid(req.getCredential().getMchId());
        request.setOutTradeNo(req.getOutTradeNo());
        request.setOutRefundNo(req.getOutRefundNo());
        request.setReason(StrUtil.blankToDefault(req.getReason(), "退款"));
        request.setNotifyUrl(req.getNotifyUrl());
        ApiAmountReq amount = new ApiAmountReq();
        amount.setRefund(req.getRefundAmount().intValue());
        amount.setTotal(req.getTotalAmount().intValue());
        amount.setCurrency(CURRENCY_CNY);
        request.setAmount(amount);
        try {
            var result = DouyinSdkConfig.refundService(req.getCredential()).create(request);
            return new DouyinRefundResp()
                    .setOutRefundNo(req.getOutRefundNo())
                    .setRefundId(result.getRefundId())
                    .setRefundStatus(result.getRefundStatus())
                    .setFinishTime(result.getSuccessTime());
        } catch (DouyinpayException e) {
            log.error("抖音退款申请失败: outRefundNo={}", req.getOutRefundNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.douyinRefundFailed", e.getMessage());
        }
    }
}
