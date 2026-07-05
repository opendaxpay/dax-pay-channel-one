package cn.daxpay.open.channel.douyin.service.close;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.req.DouyinCloseReq;
import cn.daxpay.open.channel.douyin.resp.DouyinCloseResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import com.douyinpay.api.payments.nativepay.models.ApiCloseOrderRequest;
import com.douyinpay.exception.DouyinpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 抖音通道关单服务
///
/// 关单接口不区分支付方式, 统一使用 NATIVE Service 调用。
@Slf4j
@Service
public class DouyinCloseService {

    /// 关闭抖音支付订单
    public DouyinCloseResp close(DouyinCloseReq req) {
        ApiCloseOrderRequest request = new ApiCloseOrderRequest();
        request.setMchid(req.getCredential().getMchId());
        request.setOutTradeNo(req.getOutTradeNo());
        try {
            DouyinSdkConfig.nativeService(req.getCredential()).closeOrder(request);
            return new DouyinCloseResp().setOutTradeNo(req.getOutTradeNo());
        } catch (DouyinpayException e) {
            log.error("抖音支付关单失败: outTradeNo={}", req.getOutTradeNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.douyinCloseFailed", e.getMessage());
        }
    }
}
