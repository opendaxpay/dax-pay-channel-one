package cn.daxpay.open.channel.wechat.service.direct;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatSyncReq;
import cn.daxpay.open.channel.wechat.resp.WechatSyncResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/// # 微信通道支付同步服务
///
/// 调用微信 V3 `查询订单` 接口查询订单状态, 原样回传字段, 不做业务状态映射。
/// outTradeNo 与 transactionId 至少传一个; 同时传时优先使用 transactionId(微信支付订单号)。
/// trade_state 映射由主应用完成。
@Slf4j
@Service
public class WechatDirectSyncService {

    /// V3 时间格式(RFC3339)
    private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /// 支付同步(查询微信订单状态)
    public WechatSyncResp sync(WechatSyncReq req) {
        WxPayService service = WechatSdkConfig.buildService(req.getCredential());
        try {
            // 优先用 transactionId 查询
            WxPayOrderQueryV3Result result = StrUtil.isNotBlank(req.getTransactionId())
                    ? service.queryOrderV3(req.getTransactionId(), null)
                    : service.queryOrderV3(null, req.getOutTradeNo());

            WechatSyncResp resp = new WechatSyncResp();
            resp.setTradeState(result.getTradeState());
            resp.setTradeStateDesc(result.getTradeStateDesc());
            resp.setTransactionId(result.getTransactionId());
            resp.setOutTradeNo(result.getOutTradeNo());

            // 支付完成时间(RFC3339 → OffsetDateTime)
            if (StrUtil.isNotBlank(result.getSuccessTime())) {
                resp.setSuccessTime(OffsetDateTime.parse(result.getSuccessTime(), RFC3339_FORMATTER));
            }
            // 金额(支付成功时返回, 分)
            if (result.getAmount() != null) {
                if (result.getAmount().getTotal() != null) {
                    resp.setTotalAmount(result.getAmount().getTotal().longValue());
                }
                if (result.getAmount().getPayerTotal() != null) {
                    resp.setPayerTotal(result.getAmount().getPayerTotal().longValue());
                }
            }
            // 用户标识(支付成功时返回)
            if (result.getPayer() != null) {
                resp.setOpenId(result.getPayer().getOpenid());
            }
            return resp;
        } catch (WxPayException e) {
            log.error("微信订单查询失败: outTradeNo={}, transactionId={}, err={}",
                    req.getOutTradeNo(), req.getTransactionId(), e.getMessage());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatOrderQueryFailed", e.getMessage());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
