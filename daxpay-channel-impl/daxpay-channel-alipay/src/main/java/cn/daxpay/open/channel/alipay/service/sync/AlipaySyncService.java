package cn.daxpay.open.channel.alipay.service.sync;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.alipay.req.AlipaySyncReq;
import cn.daxpay.open.channel.alipay.resp.AlipaySyncResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.common.util.PayUtil;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

/// # 支付宝通道支付同步服务
///
/// 调用 `alipay.trade.query` 查询支付宝订单状态, 原样回传字段, 不做业务状态映射。
/// outTradeNo 与 tradeNo 至少传一个; 同时传时优先使用 tradeNo。
@Slf4j
@Service
public class AlipaySyncService {

    /// 支付同步(查询支付宝订单状态)
    public AlipaySyncResp sync(AlipaySyncReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(req.getOutTradeNo());
        if (StrUtil.isNotBlank(req.getTradeNo())) {
            model.setTradeNo(req.getTradeNo());
        }
        request.setBizModel(model);
        try {
            AlipayTradeQueryResponse response = client.execute(request);
            AlipaySyncResp resp = new AlipaySyncResp();
            resp.setCode(response.getCode());
            resp.setSubCode(response.getSubCode());
            resp.setSubMsg(response.getSubMsg());
            resp.setTradeStatus(response.getTradeStatus());
            resp.setTradeNo(response.getTradeNo());
            resp.setOutTradeNo(response.getOutTradeNo());
            // 付款时间(Date → OffsetDateTime)
            Date sendPayDate = response.getSendPayDate();
            if (sendPayDate != null) {
                resp.setSendPayDate(OffsetDateTime.ofInstant(sendPayDate.toInstant(), ZoneId.systemDefault()));
            }
            resp.setBuyerUserId(response.getBuyerUserId());
            resp.setBuyerOpenId(response.getBuyerOpenId());
            // 实付金额(元 → 分)
            String buyerPayAmount = response.getBuyerPayAmount();
            if (StrUtil.isNotBlank(buyerPayAmount)) {
                resp.setBuyerPayAmount((long) PayUtil.conversionYuanToFenHalfUp(buyerPayAmount));
            }
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝订单查询失败: outTradeNo={}, tradeNo={}, err={}",
                    req.getOutTradeNo(), req.getTradeNo(), e.getErrMsg());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayOrderQueryFailed", e.getErrMsg());
        }
    }
}
