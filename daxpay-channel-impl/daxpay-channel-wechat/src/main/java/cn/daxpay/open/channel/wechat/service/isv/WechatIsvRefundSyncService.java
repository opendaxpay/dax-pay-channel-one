package cn.daxpay.open.channel.wechat.service.isv;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatRefundSyncReq;
import cn.daxpay.open.channel.wechat.resp.WechatRefundSyncResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.request.WxPayRefundQueryV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/// # 微信服务商通道退款同步服务
///
/// 调用微信 V3 服务商 `查询单笔退款` 接口(refundPartnerQueryV3)查询退款状态。
/// 与直连模式差异: 请求体 [WxPayRefundQueryV3Request] 显式设置 sub_mchid(特约商户号)。
@Slf4j
@Service
public class WechatIsvRefundSyncService {

    /// V3 时间格式(RFC3339)
    private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /// 服务商退款同步(查询退款状态)
    public WechatRefundSyncResp sync(WechatRefundSyncReq req) {
        log.info("微信服务商通道收到退款同步请求: outRefundNo={}", req.getOutRefundNo());

        WxPayService service = WechatSdkConfig.buildService(req.getCredential());
        try {
            // 服务商退款查询: 需显式带 sub_mchid
            WxPayRefundQueryV3Request request = new WxPayRefundQueryV3Request();
            request.setOutRefundNo(req.getOutRefundNo());
            request.setSubMchid(req.getCredential().getSubMchId());
            WxPayRefundQueryV3Result result = service.refundPartnerQueryV3(request);

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
            if (Objects.nonNull(result.getAmount())) {
                if (Objects.nonNull(result.getAmount().getRefund())) {
                    resp.setRefundAmount(result.getAmount().getRefund().longValue());
                }
                if (Objects.nonNull(result.getAmount().getPayerRefund())) {
                    resp.setPayerRefund(result.getAmount().getPayerRefund().longValue());
                }
            }
            return resp;
        } catch (WxPayException e) {
            log.error("微信服务商退款查询失败: outRefundNo={}, errCode={}, errMsg={}",
                    req.getOutRefundNo(), e.getErrCode(), e.getMessage());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatRefundQueryFailed", e.getMessage());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
