package cn.daxpay.open.channel.wechat.service.direct;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatRefundSyncReq;
import cn.daxpay.open.channel.wechat.resp.WechatRefundSyncResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/// # 微信通道退款同步服务
///
/// 调用微信 V3 `查询单笔退款` 接口查询退款状态。
/// 退款发起时若 status=PROCESSING/ABNORMAL(未终态), 需经本接口确认退款最终结果。
/// refund status 映射由主应用完成。
@Slf4j
@Service
public class WechatDirectRefundSyncService {

    /// V3 时间格式(RFC3339)
    private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /// 退款同步(查询退款状态)
    public WechatRefundSyncResp sync(WechatRefundSyncReq req) {
        log.info("微信通道收到退款同步请求: outRefundNo={}", req.getOutRefundNo());

        WxPayService service = WechatSdkConfig.buildService(req.getCredential());
        try {
            WxPayRefundQueryV3Result result = service.refundQueryV3(req.getOutRefundNo());
            var resp = new WechatRefundSyncResp();
            resp.setStatus(result.getStatus());
            resp.setRefundId(result.getRefundId());
            resp.setOutRefundNo(result.getOutRefundNo());
            resp.setTransactionId(result.getTransactionId());
            resp.setOutTradeNo(result.getOutTradeNo());
            // 退款完成时间
            if (StrUtil.isNotBlank(result.getSuccessTime())) {
                resp.setFinishTime(OffsetDateTime.parse(result.getSuccessTime(), RFC3339_FORMATTER));
            }
            // 金额(分)
            if (result.getAmount() != null) {
                if (result.getAmount().getRefund() != null) {
                    resp.setRefundAmount(result.getAmount().getRefund().longValue());
                }
                if (result.getAmount().getPayerRefund() != null) {
                    resp.setPayerRefund(result.getAmount().getPayerRefund().longValue());
                }
            }
            return resp;
        } catch (WxPayException e) {
            log.error("微信退款查询失败: outRefundNo={}, errCode={}, errMsg={}",
                    req.getOutRefundNo(), e.getErrCode(), e.getMessage());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatRefundQueryFailed", e.getMessage());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
