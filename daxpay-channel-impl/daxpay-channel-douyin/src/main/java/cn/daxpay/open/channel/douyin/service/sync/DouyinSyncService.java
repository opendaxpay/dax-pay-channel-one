package cn.daxpay.open.channel.douyin.service.sync;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.req.DouyinSyncReq;
import cn.daxpay.open.channel.douyin.resp.DouyinSyncResp;
import cn.hutool.core.util.StrUtil;
import com.douyinpay.api.payments.nativepay.models.ApiQueryOrderByOutTradeNoRequest;
import com.douyinpay.exception.DouyinpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 抖音通道支付同步服务
///
/// 查单接口不区分支付方式, 统一使用 NATIVE Service 调用。
/// 抖音订单不存在(ORDER_NOT_EXIST) 视为已关闭(客户未操作, 订单未创建)。
@Slf4j
@Service
public class DouyinSyncService {

    /// 抖音错误码: 订单不存在
    private static final String ORDER_NOT_EXIST = "ORDER_NOT_EXIST";

    /// 同步抖音支付订单状态
    public DouyinSyncResp sync(DouyinSyncReq req) {
        ApiQueryOrderByOutTradeNoRequest request = new ApiQueryOrderByOutTradeNoRequest();
        request.setMchid(req.getCredential().getMchId());
        request.setOutTradeNo(req.getOutTradeNo());
        try {
            var result = DouyinSdkConfig.nativeService(req.getCredential()).queryOrderByOutTradeNo(request);
            DouyinSyncResp resp = new DouyinSyncResp()
                    .setOutTradeNo(req.getOutTradeNo())
                    .setTransactionId(result.getTransactionId())
                    .setTradeState(result.getTradeState())
                    .setSuccessTime(result.getSuccessTime());
            if (result.getAmount() != null) {
                resp.setTotalAmount(result.getAmount().getTotal().longValue());
            }
            if (result.getPayer() != null) {
                resp.setOpenid(result.getPayer().getOpenid());
            }
            return resp;
        } catch (DouyinpayException e) {
            log.error("抖音支付订单查询失败: outTradeNo={}", req.getOutTradeNo(), e);
            DouyinSyncResp resp = new DouyinSyncResp().setOutTradeNo(req.getOutTradeNo());
            // 订单不存在视为已关闭
            if (StrUtil.contains(e.getMessage(), ORDER_NOT_EXIST)) {
                return resp.setTradeState("CLOSED");
            }
            return resp.setErrorCode(null)
                    .setErrorMsg(e.getMessage());
        }
    }
}
