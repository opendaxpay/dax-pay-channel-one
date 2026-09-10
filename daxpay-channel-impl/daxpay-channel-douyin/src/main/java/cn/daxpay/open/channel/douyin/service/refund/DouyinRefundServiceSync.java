package cn.daxpay.open.channel.douyin.service.refund;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.req.DouyinRefundSyncReq;
import cn.daxpay.open.channel.douyin.resp.DouyinRefundSyncResp;
import cn.hutool.core.util.StrUtil;
import com.douyinpay.api.refund.model.ApiQueryByOutRefundNoRequest;
import com.douyinpay.exception.DouyinpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Objects;

/// # 抖音通道退款同步服务
///
/// 通过退款单号查询退款最终状态。
@Slf4j
@Service
public class DouyinRefundServiceSync {

    /// 同步抖音退款状态
    public DouyinRefundSyncResp sync(DouyinRefundSyncReq req) {
        ApiQueryByOutRefundNoRequest request = new ApiQueryByOutRefundNoRequest();
        request.setMchid(req.getCredential().getMchId());
        request.setOutRefundNo(req.getOutRefundNo());
        try {
            var result = DouyinSdkConfig.refundService(req.getCredential()).queryByOutRefundNo(request);
            DouyinRefundSyncResp resp = new DouyinRefundSyncResp()
                    .setOutRefundNo(req.getOutRefundNo())
                    .setRefundId(result.getRefundId())
                    .setRefundStatus(result.getRefundStatus())
                    .setFinishTime(result.getSuccessTime());
            if (Objects.nonNull(result.getAmount())) {
                resp.setRefundAmount(result.getAmount().getRefund().longValue());
            }
            return resp;
        } catch (DouyinpayException e) {
            log.error("抖音退款查询失败: outRefundNo={}", req.getOutRefundNo(), e);
            return new DouyinRefundSyncResp()
                    .setOutRefundNo(req.getOutRefundNo())
                    .setErrorCode(null)
                    .setErrorMsg(e.getMessage());
        }
    }
}
