package cn.daxpay.open.channel.wechat.service.isv;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatSyncReq;
import cn.daxpay.open.channel.wechat.resp.WechatSyncResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.result.WxPayPartnerOrderQueryV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/// # 微信服务商通道支付同步服务
///
/// 调用微信 V3 服务商 `查询订单` 接口(queryPartnerOrderV3)查询订单状态。
/// 与直连模式差异: payer 的 openid 优先取 sub_openid, 空则回退 sp_openid(对标商业版 WechatSubPaySyncService)。
@Slf4j
@Service
public class WechatIsvSyncService {

    /// V3 时间格式(RFC3339)
    private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /// 服务商支付同步(查询微信订单状态)
    public WechatSyncResp sync(WechatSyncReq req) {
        WxPayService service = WechatSdkConfig.buildService(req.getCredential());
        try {
            // 优先用 transactionId 查询
            WxPayPartnerOrderQueryV3Result result = StrUtil.isNotBlank(req.getTransactionId())
                    ? service.queryPartnerOrderV3(req.getTransactionId(), null)
                    : service.queryPartnerOrderV3(null, req.getOutTradeNo());

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
            if (Objects.nonNull(result.getAmount())) {
                if (Objects.nonNull(result.getAmount().getTotal())) {
                    resp.setTotalAmount(result.getAmount().getTotal().longValue());
                }
                if (Objects.nonNull(result.getAmount().getPayerTotal())) {
                    resp.setPayerTotal(result.getAmount().getPayerTotal().longValue());
                }
            }
            // 用户标识: sub_openid 优先, 空则回退 sp_openid
            if (Objects.nonNull(result.getPayer())) {
                String openid = StrUtil.isNotBlank(result.getPayer().getSubOpenid())
                        ? result.getPayer().getSubOpenid()
                        : result.getPayer().getSpOpenid();
                resp.setOpenId(openid);
            }
            return resp;
        } catch (WxPayException e) {
            log.error("微信服务商订单查询失败: outTradeNo={}, transactionId={}, err={}",
                    req.getOutTradeNo(), req.getTransactionId(), e.getMessage());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatOrderQueryFailed", e.getMessage());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }
}
