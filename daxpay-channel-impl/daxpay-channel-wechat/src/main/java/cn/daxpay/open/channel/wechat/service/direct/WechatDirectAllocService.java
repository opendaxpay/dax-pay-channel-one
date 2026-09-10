package cn.daxpay.open.channel.wechat.service.direct;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatAllocReq;
import cn.daxpay.open.channel.wechat.resp.WechatAllocResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingV3Request;
import com.github.binarywang.wxpay.bean.profitsharing.result.ProfitSharingV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// # 微信通道分账服务
///
/// 分账发起(V3 profitsharing/orders)与查询(profitsharing/orders/{out_order_no})。
/// 状态映射见主应用 [cn.daxpay.open.channel.wechat.service.payment.alloc.WechatAllocService]。
/// 金额单位: 分(微信分账金额单位即分, 无需转换)。
@Slf4j
@Service
public class WechatDirectAllocService {

    /// 微信时间格式(RFC3339 或 yyyy-MM-dd HH:mm:ss)
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /// 发起分账(V3 profitsharing/orders, unfreezeUnsplit=true 自动解冻剩余)
    public WechatAllocResp alloc(WechatAllocReq req) {
        WxPayService wxPayService = WechatSdkConfig.buildService(req.getCredential());
        ProfitSharingV3Request v3Req = new ProfitSharingV3Request();
        v3Req.setTransactionId(req.getTransactionId());
        v3Req.setOutOrderNo(req.getOutOrderNo());
        v3Req.setUnfreezeUnsplit(true);
        // appid 必传: 接收方类型含 PERSONAL_OPENID 时微信强制校验(openid 是 appid 维度账号),
        // MERCHANT_ID 类型下多传无害; WxJava profitSharingV3 直接序列化请求体, 不会从 config 自动补
        v3Req.setAppid(req.getCredential().getWxAppId());
        // 接收方列表
        List<ProfitSharingV3Request.Receiver> receivers = new ArrayList<>();
        if (Objects.nonNull(req.getReceivers())) {
            for (WechatAllocReq.Receiver r : req.getReceivers()) {
                ProfitSharingV3Request.Receiver receiver = new ProfitSharingV3Request.Receiver();
                receiver.setType(r.getType());
                receiver.setAccount(r.getAccount());
                receiver.setAmount(r.getAmount().intValue());
                receiver.setName(r.getName());
                receiver.setDescription(StrUtil.blankToDefault(r.getDescription(), "订单分账"));
                receivers.add(receiver);
            }
        }
        v3Req.setReceivers(receivers);
        try {
            ProfitSharingV3Result result = wxPayService.getProfitSharingService().profitSharingV3(v3Req);
            WechatAllocResp resp = new WechatAllocResp();
            resp.setTransactionId(result.getTransactionId());
            resp.setState(result.getState());
            return resp;
        } catch (WxPayException e) {
            log.error("微信分账发起失败: allocNo={}", req.getOutOrderNo(), e);
            WechatAllocResp resp = new WechatAllocResp();
            resp.setErrorCode(e.getErrCode());
            resp.setErrorMsg(StrUtil.blankToDefault(e.getErrCodeDes(), e.getMessage()));
            return resp;
        }
    }

    /// 分账同步查询(V3 profitsharing/orders/{out_order_no})
    public WechatAllocResp sync(WechatAllocReq req) {
        WxPayService wxPayService = WechatSdkConfig.buildService(req.getCredential());
        try {
            ProfitSharingV3Result result = wxPayService.getProfitSharingService()
                    .profitSharingQueryV3(req.getOutOrderNo(), req.getTransactionId());
            WechatAllocResp resp = new WechatAllocResp();
            resp.setTransactionId(result.getTransactionId());
            resp.setState(result.getState());
            // 映射逐明细结果
            List<WechatAllocResp.ReceiverResult> receiverResults = new ArrayList<>();
            if (Objects.nonNull(result.getReceivers())) {
                for (ProfitSharingV3Result.Receiver r : result.getReceivers()) {
                    WechatAllocResp.ReceiverResult rr = new WechatAllocResp.ReceiverResult();
                    rr.setAccount(r.getAccount());
                    rr.setAmount(Objects.nonNull(r.getAmount()) ? r.getAmount().longValue() : null);
                    rr.setResult(r.getResult());
                    rr.setFailReason(r.getFailReason());
                    rr.setFinishTime(parseDate(r.getFinishTime()));
                    receiverResults.add(rr);
                }
            }
            resp.setReceivers(receiverResults);
            return resp;
        } catch (WxPayException e) {
            log.error("微信分账查询失败: allocNo={}", req.getOutOrderNo(), e);
            WechatAllocResp resp = new WechatAllocResp();
            resp.setErrorCode(e.getErrCode());
            resp.setErrorMsg(StrUtil.blankToDefault(e.getErrCodeDes(), e.getMessage()));
            return resp;
        }
    }

    /// 解析微信时间字符串(yyyy-MM-dd HH:mm:ss 东八区)
    private OffsetDateTime parseDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateStr.replace(" ", "T") + "+08:00",
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            // 兼容 yyyy-MM-dd HH:mm:ss 格式
            try {
                return java.time.LocalDateTime.parse(dateStr, DATE_FMT).atOffset(ZoneOffset.ofHours(8));
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
