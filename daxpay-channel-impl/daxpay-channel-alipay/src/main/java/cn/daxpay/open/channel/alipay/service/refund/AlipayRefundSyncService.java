package cn.daxpay.open.channel.alipay.service.refund;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.alipay.req.AlipayRefundSyncReq;
import cn.daxpay.open.channel.alipay.resp.AlipayRefundSyncResp;
import cn.daxpay.open.platform.common.util.PayUtil;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

/// # 支付宝通道退款同步服务
///
/// 调用 `alipay.trade.fastpay.refund.query` 查询退款状态。
/// 退款发起时若 `fund_change=N`(资金未即时变动), 需经本接口确认退款最终结果。
///
/// 查询结果 `refund_status=REFUND_SUCCESS` 表示退款成功, 由主应用完成业务状态映射。
@Slf4j
@Service
public class AlipayRefundSyncService {

    /// 退款成功状态码
    private static final String REFUND_SUCCESS = "REFUND_SUCCESS";

    /// 退款同步(查询退款状态)
    public AlipayRefundSyncResp sync(AlipayRefundSyncReq req) {
        log.info("支付宝通道收到退款同步请求: outTradeNo={}, outRequestNo={}",
                req.getOutTradeNo(), req.getOutRequestNo());

        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());

        var model = new AlipayTradeFastpayRefundQueryModel();
        model.setOutTradeNo(req.getOutTradeNo());
        if (StrUtil.isNotBlank(req.getTradeNo())) {
            model.setTradeNo(req.getTradeNo());
        }
        model.setOutRequestNo(req.getOutRequestNo());

        var request = new AlipayTradeFastpayRefundQueryRequest();
        request.setBizModel(model);

        // 服务商模式: 注入应用授权令牌
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }

        try {
            AlipayTradeFastpayRefundQueryResponse response = AlipaySdkConfig.execute(client, req.getCredential(), request);
            var resp = new AlipayRefundSyncResp();
            resp.setCode(response.getCode());
            resp.setSubCode(response.getSubCode());
            resp.setSubMsg(response.getSubMsg());
            resp.setRefundStatus(response.getRefundStatus());
            resp.setOutTradeNo(response.getOutTradeNo());
            resp.setTradeNo(response.getTradeNo());
            resp.setOutRequestNo(response.getOutRequestNo());

            // 退款完成时间(Date → OffsetDateTime)
            Date gmtRefundPay = response.getGmtRefundPay();
            if (gmtRefundPay != null) {
                resp.setFinishTime(OffsetDateTime.ofInstant(gmtRefundPay.toInstant(), ZoneId.systemDefault()));
            }

            // 退款金额(元 → 分)
            String refundAmount = response.getRefundAmount();
            if (StrUtil.isNotBlank(refundAmount)) {
                resp.setRefundAmount((long) PayUtil.conversionYuanToFenHalfUp(refundAmount));
            }
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝退款查询失败: outTradeNo={}, outRequestNo={}, err={}",
                    req.getOutTradeNo(), req.getOutRequestNo(), e.getErrMsg());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayRefundQueryFailed", e.getErrMsg());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
