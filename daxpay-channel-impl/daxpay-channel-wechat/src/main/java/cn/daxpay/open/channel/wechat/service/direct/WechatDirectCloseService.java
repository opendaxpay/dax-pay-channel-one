package cn.daxpay.open.channel.wechat.service.direct;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatCloseReq;
import cn.daxpay.open.channel.wechat.resp.WechatCloseResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 微信通道关闭服务
///
/// 调用微信 V3 `关闭订单` 接口终结未支付订单。
/// 微信关单仅支持 out_trade_no, 不支持 transaction_id。
///
/// 失败兜底(参照商业版):
/// - `ORDER_NOT_EXIST`(订单不存在) → 视为已关闭成功
/// - `ORDER_CLOSED`(订单已关闭) → 视为成功
/// - 其他 → 抛业务异常
@Slf4j
@Service
public class WechatDirectCloseService {

    /// 微信错误码: 订单不存在
    private static final String ORDER_NOT_EXIST = "ORDER_NOT_EXIST";
    /// 微信错误码: 订单已关闭
    private static final String ORDER_CLOSED = "ORDER_CLOSED";

    /// 关闭微信订单
    public WechatCloseResp close(WechatCloseReq req) {
        WxPayService service = WechatSdkConfig.buildService(req.getCredential());
        try {
            service.closeOrderV3(req.getOutTradeNo());
            return toResp(req);
        } catch (WxPayException e) {
            // 兜底: 订单不存在 / 订单已关闭 视为关单成功
            String errCode = e.getErrCode();
            if (ORDER_NOT_EXIST.equals(errCode) || ORDER_CLOSED.equals(errCode)) {
                log.info("微信关单兜底成功: outTradeNo={}, errCode={}", req.getOutTradeNo(), errCode);
                return toResp(req);
            }
            log.error("微信关闭订单失败: outTradeNo={}, errCode={}, errMsg={}",
                    req.getOutTradeNo(), errCode, e.getMessage());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatCloseOrderFailed", e.getMessage());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
    }

    /// 组装关闭响应(订单号透传请求)
    private static WechatCloseResp toResp(WechatCloseReq req) {
        WechatCloseResp resp = new WechatCloseResp();
        resp.setOutTradeNo(req.getOutTradeNo());
        resp.setTransactionId(req.getTransactionId());
        return resp;
    }
}
