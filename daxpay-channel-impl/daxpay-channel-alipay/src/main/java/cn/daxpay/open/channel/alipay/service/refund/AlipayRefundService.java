package cn.daxpay.open.channel.alipay.service.refund;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.alipay.req.AlipayRefundReq;
import cn.daxpay.open.channel.alipay.resp.AlipayRefundResp;
import cn.daxpay.open.platform.common.util.PayUtil;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.SdkCallException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/// # 支付宝通道退款服务
///
/// 调用 `alipay.trade.refund` 发起退款。
/// 一次退款请求由 outRequestNo 唯一标识, 同一订单可多次部分退款, 每次 outRequestNo 不可重复。
///
/// 资金变动判定(参照商业版 AlipayRefundService):
/// - `fund_change=Y` → 资金已变动, 退款即时成功(complete=true)
/// - `fund_change=N` 或缺失 → 资金未变动, 需主应用经退款同步查询确认最终状态(complete=false)
@Slf4j
@Service
public class AlipayRefundService {

    /// 资金变动标志: 已变动
    private static final String FUND_CHANGE_Y = "Y";
    /// 银行卡冲退信息查询选项, 传输此值才会触发银行卡退款的异步回调
    private static final String QUERY_OPTION_DEPOSIT_BACK = "deposit_back_info";

    /// 通道退款
    ///
    /// 金额单位转换: 请求中为分, 调用 SDK 时转为元(保留两位小数)。
    public AlipayRefundResp refund(AlipayRefundReq req) {
        log.info("支付宝通道收到退款请求: outTradeNo={}, outRequestNo={}, refundAmount={}",
                req.getOutTradeNo(), req.getOutRequestNo(), req.getRefundAmount());

        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());

        // 金额单位转换: 分(整型) → 元(保留两位小数)
        String refundAmount = PayUtil.conversionFenToYuan(req.getRefundAmount()).toPlainString();

        var model = new AlipayTradeRefundModel();
        model.setOutTradeNo(req.getOutTradeNo());
        if (StrUtil.isNotBlank(req.getTradeNo())) {
            model.setTradeNo(req.getTradeNo());
        }
        model.setOutRequestNo(req.getOutRequestNo());
        model.setRefundAmount(refundAmount);
        // 银行卡冲退信息, 只有传输了此值才会触发退款回调
        model.setQueryOptions(List.of(QUERY_OPTION_DEPOSIT_BACK));

        var request = new AlipayTradeRefundRequest();
        request.setBizModel(model);

        // 服务商模式: 注入应用授权令牌
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }

        AlipayRefundResp resp = new AlipayRefundResp();
        resp.setOutTradeNo(req.getOutTradeNo());
        resp.setTradeNo(req.getTradeNo());
        resp.setOutRequestNo(req.getOutRequestNo());
        // 默认未终态完成, fund_change=Y 时覆盖为 true
        resp.setComplete(false);

        try {
            AlipayTradeRefundResponse response = AlipaySdkConfig.execute(client, req.getCredential(), request);
            if (!response.isSuccess()) {
                log.error("支付宝退款失败: code={}, subCode={}, subMsg={}",
                        response.getCode(), response.getSubCode(), response.getSubMsg());
                throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                        "channel.error.alipayRefundCallFailed",
                        StrUtil.blankToDefault(response.getSubMsg(), response.getMsg()));
            }
            // 透传网关返回字段
            resp.setOutTradeNo(response.getOutTradeNo());
            resp.setTradeNo(response.getTradeNo());
            resp.setFundChange(response.getFundChange());
            resp.setBuyerUserId(response.getBuyerUserId());
            resp.setBuyerOpenId(response.getBuyerOpenId());

            // fund_change=Y → 资金已变动, 退款即时成功
            if (FUND_CHANGE_Y.equals(response.getFundChange())) {
                resp.setComplete(true);
                Date gmtRefundPay = response.getGmtRefundPay();
                if (Objects.nonNull(gmtRefundPay)) {
                    resp.setFinishTime(OffsetDateTime.ofInstant(gmtRefundPay.toInstant(), ZoneId.systemDefault()));
                }
            }
        } catch (ChannelServiceException e) {
            // 业务异常直接透传, 避免被包装成 SDK 调用异常
            throw e;
        } catch (AlipayApiException e) {
            log.error("支付宝退款调用异常: outTradeNo={}, outRequestNo={}, err={}",
                    req.getOutTradeNo(), req.getOutRequestNo(), e.getErrMsg());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayRefundCallFailed", e.getErrMsg());
        } catch (Exception e) {
            throw new SdkCallException(e.getMessage(), e);
        }
        return resp;
    }
}
