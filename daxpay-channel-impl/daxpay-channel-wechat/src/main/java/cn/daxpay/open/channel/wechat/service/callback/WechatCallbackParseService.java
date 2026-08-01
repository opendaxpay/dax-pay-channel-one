package cn.daxpay.open.channel.wechat.service.callback;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatCallbackParseReq;
import cn.daxpay.open.channel.wechat.resp.WechatCallbackParseResp;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyV3Result;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyV3Result;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 微信回调验签解析服务
///
/// 主应用接收到微信异步通知后, 将原始 header + body + 凭证转发到本服务,
/// 使用 WxJava [WxPayService] 的 parseOrderNotifyV3Result / parseRefundNotifyV3Result
/// 完成平台证书验签与 AES 解密。
@Slf4j
@Service
public class WechatCallbackParseService {

    /// 解析支付回调(验签 + 解密)
    public WechatCallbackParseResp parsePay(WechatCallbackParseReq req) {
        try {
            // 回调验签不依赖 appId, 用宽松构建(无需通道应用)
            WxPayService service = WechatSdkConfig.buildCallbackService(req.getCredential());
            SignatureHeader header = this.buildHeader(req);
            WxPayNotifyV3Result notifyResult = service.parseOrderNotifyV3Result(req.getBody(), header);
            // 解密后的业务数据在 getResult() 中
            WxPayNotifyV3Result.DecryptNotifyResult result = notifyResult.getResult();
            WechatCallbackParseResp resp = new WechatCallbackParseResp()
                    .setVerified(true)
                    .setTradeType("PAY")
                    .setOutTradeNo(result.getOutTradeNo())
                    .setTransactionId(result.getTransactionId())
                    .setTradeState(result.getTradeState())
                    .setSuccessTime(result.getSuccessTime());
            if (result.getAmount() != null) {
                resp.setAmount(result.getAmount().getTotal().longValue());
            }
            if (result.getPayer() != null) {
                resp.setOpenid(result.getPayer().getOpenid());
            }
            return resp;
        } catch (Exception e) {
            log.error("微信支付回调验签解析失败", e);
            return new WechatCallbackParseResp().setVerified(false);
        }
    }

    /// 解析退款回调(验签 + 解密)
    public WechatCallbackParseResp parseRefund(WechatCallbackParseReq req) {
        try {
            // 回调验签不依赖 appId, 用宽松构建(无需通道应用)
            WxPayService service = WechatSdkConfig.buildCallbackService(req.getCredential());
            SignatureHeader header = this.buildHeader(req);
            WxPayRefundNotifyV3Result notifyResult = service.parseRefundNotifyV3Result(req.getBody(), header);
            // 解密后的业务数据在 getResult() 中
            WxPayRefundNotifyV3Result.DecryptNotifyResult result = notifyResult.getResult();
            WechatCallbackParseResp resp = new WechatCallbackParseResp()
                    .setVerified(true)
                    .setTradeType("REFUND")
                    .setOutRefundNo(result.getOutRefundNo())
                    .setRefundId(result.getRefundId())
                    .setRefundStatus(result.getRefundStatus())
                    .setSuccessTime(result.getSuccessTime());
            if (result.getAmount() != null) {
                resp.setAmount(result.getAmount().getRefund().longValue());
            }
            return resp;
        } catch (Exception e) {
            log.error("微信退款回调验签解析失败", e);
            return new WechatCallbackParseResp().setVerified(false);
        }
    }

    /// 构建微信验签头
    private SignatureHeader buildHeader(WechatCallbackParseReq req) {
        return SignatureHeader.builder()
                .serial(req.getSerial())
                .nonce(req.getNonce())
                .signature(req.getSignature())
                .timeStamp(req.getTimestamp())
                .build();
    }
}
