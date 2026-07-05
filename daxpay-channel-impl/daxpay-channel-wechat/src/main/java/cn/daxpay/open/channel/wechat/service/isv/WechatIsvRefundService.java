package cn.daxpay.open.channel.wechat.service.isv;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatRefundReq;
import cn.daxpay.open.channel.wechat.resp.WechatRefundResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.request.WxPayPartnerRefundV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayRefundV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/// # 微信服务商通道退款服务
///
/// 调用微信 V3 服务商 `申请退款` 接口发起退款。
/// 与直连模式差异: 请求体用 [WxPayPartnerRefundV3Request] 并显式设置 sub_mchid(特约商户号)。
///
/// 退款状态 status:
/// - SUCCESS / CLOSED → 终态(complete=true)
/// - PROCESSING / ABNORMAL → 未终态(complete=false), 需主应用经退款同步查询确认最终状态
@Slf4j
@Service
public class WechatIsvRefundService {

    /// V3 时间格式(RFC3339)
    private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    /// 退款成功状态
    private static final String STATUS_SUCCESS = "SUCCESS";
    /// 退款关闭状态
    private static final String STATUS_CLOSED = "CLOSED";

    /// 服务商通道退款
    public WechatRefundResp refund(WechatRefundReq req) {
        log.info("微信服务商通道收到退款请求: outTradeNo={}, transactionId={}, outRefundNo={}, refundAmount={}",
                req.getOutTradeNo(), req.getTransactionId(), req.getOutRefundNo(), req.getRefundAmount());

        WxPayService service = WechatSdkConfig.buildService(req.getCredential());

        var request = new WxPayPartnerRefundV3Request();
        // 优先用 transactionId
        if (StrUtil.isNotBlank(req.getTransactionId())) {
            request.setTransactionId(req.getTransactionId());
        } else {
            request.setOutTradeNo(req.getOutTradeNo());
        }
        request.setOutRefundNo(req.getOutRefundNo());
        if (StrUtil.isNotBlank(req.getReason())) {
            request.setReason(req.getReason());
        }
        if (StrUtil.isNotBlank(req.getNotifyUrl())) {
            request.setNotifyUrl(req.getNotifyUrl());
        }
        // 服务商模式: 显式设置特约商户号
        request.setSubMchid(req.getCredential().getSubMchId());
        // 金额(分)
        var amount = new WxPayPartnerRefundV3Request.Amount();
        amount.setRefund(req.getRefundAmount().intValue());
        amount.setTotal(req.getTotalAmount().intValue());
        amount.setCurrency("CNY");
        request.setAmount(amount);

        try {
            WxPayRefundV3Result result = service.refundV3(request);
            WechatRefundResp resp = new WechatRefundResp();
            resp.setOutTradeNo(result.getOutTradeNo());
            resp.setTransactionId(result.getTransactionId());
            resp.setOutRefundNo(result.getOutRefundNo());
            resp.setRefundId(result.getRefundId());
            resp.setStatus(result.getStatus());
            // SUCCESS / CLOSED 为终态
            resp.setComplete(STATUS_SUCCESS.equals(result.getStatus())
                    || STATUS_CLOSED.equals(result.getStatus()));
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
            log.error("微信服务商退款调用失败: outRefundNo={}, errCode={}, errMsg={}",
                    req.getOutRefundNo(), e.getErrCode(), e.getMessage());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatRefundCallFailed", e.getMessage());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
