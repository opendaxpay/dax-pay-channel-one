package cn.daxpay.open.channel.alipay.service.close;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeCancelModel;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.request.AlipayTradeCancelRequest;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.response.AlipayTradeCancelResponse;
import com.alipay.api.response.AlipayTradeCloseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.alipay.req.AlipayCloseReq;
import cn.daxpay.open.channel.alipay.req.AlipaySyncReq;
import cn.daxpay.open.channel.alipay.resp.AlipayCloseResp;
import cn.daxpay.open.channel.alipay.resp.AlipaySyncResp;
import cn.daxpay.open.channel.alipay.service.sync.AlipaySyncService;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import org.springframework.stereotype.Service;

/// # 支付宝通道关闭/撤销服务
///
/// 调用 `alipay.trade.close`(交易关闭) 或 `alipay.trade.cancel`(交易撤销) 终结未支付订单,
/// 由 [AlipayCloseReq.useCancel] 决定调用方式。
///
/// 失败兜底(参照商业版 AlipayCloseService):
/// - `ACQ.TRADE_STATUS_ERROR`(当前交易状态不支持此操作) → 调 [AlipaySyncService] 查询网关真实状态,
///   已关闭(TRADE_CLOSED)视为成功
/// - `ACQ.TRADE_NOT_EXIST`(交易不存在) → 视为已关闭成功
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayCloseService {

    private final AlipaySyncService alipaySyncService;

    // 支付宝交易状态: 未付款超时关闭或支付完成后全额退款
    private static final String TRADE_CLOSED = "TRADE_CLOSED";
    // 业务码: 当前交易状态不支持此操作
    private static final String ACQ_TRADE_STATUS_ERROR = "ACQ.TRADE_STATUS_ERROR";
    // 业务码: 交易不存在
    private static final String ACQ_TRADE_NOT_EXIST = "ACQ.TRADE_NOT_EXIST";

    /// 关闭或撤销支付宝订单
    public AlipayCloseResp close(AlipayCloseReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        return req.isUseCancel()
                ? doCancel(client, req)
                : doClose(client, req);
    }

    /// 交易关闭(alipay.trade.close)
    ///
    /// 仅未支付订单可关闭, 商户无需额外签约权限。
    private AlipayCloseResp doClose(AlipayClient client, AlipayCloseReq req) {
        AlipayTradeCloseModel model = new AlipayTradeCloseModel();
        model.setOutTradeNo(req.getOutTradeNo());
        if (StrUtil.isNotBlank(req.getTradeNo())) {
            model.setTradeNo(req.getTradeNo());
        }
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        request.setBizModel(model);
        try {
            AlipayTradeCloseResponse response = client.execute(request);
            if (response.isSuccess()) {
                return toResp(req, response.getCode(), response.getSubCode(), response.getSubMsg());
            }
            // 兜底处理
            return handleFailure(req, response.getSubCode(), response.getSubMsg(),
                    "channel.error.alipayCloseOrderFailed");
        } catch (AlipayApiException e) {
            log.error("支付宝关闭订单失败: outTradeNo={}, tradeNo={}, err={}",
                    req.getOutTradeNo(), req.getTradeNo(), e.getErrMsg());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayCloseOrderFailed", e.getErrMsg());
        }
    }

    /// 交易撤销(alipay.trade.cancel)
    ///
    /// 若用户已支付会将资金退回, 限制一天内有效, 需专门签约权限。
    private AlipayCloseResp doCancel(AlipayClient client, AlipayCloseReq req) {
        AlipayTradeCancelModel model = new AlipayTradeCancelModel();
        model.setOutTradeNo(req.getOutTradeNo());
        if (StrUtil.isNotBlank(req.getTradeNo())) {
            model.setTradeNo(req.getTradeNo());
        }
        AlipayTradeCancelRequest request = new AlipayTradeCancelRequest();
        request.setBizModel(model);
        try {
            AlipayTradeCancelResponse response = client.execute(request);
            if (response.isSuccess()) {
                return toResp(req, response.getCode(), response.getSubCode(), response.getSubMsg());
            }
            // 兜底处理
            return handleFailure(req, response.getSubCode(), response.getSubMsg(),
                    "channel.error.alipayOrderCancelFailed");
        } catch (AlipayApiException e) {
            log.error("支付宝撤销订单失败: outTradeNo={}, tradeNo={}, err={}",
                    req.getOutTradeNo(), req.getTradeNo(), e.getErrMsg());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayOrderCancelFailed", e.getErrMsg());
        }
    }

    /// 失败兜底处理
    ///
    /// - `ACQ.TRADE_STATUS_ERROR` → 查询网关真实状态, 已关闭(TRADE_CLOSED)视为成功
    /// - `ACQ.TRADE_NOT_EXIST` → 视为已关闭成功
    /// - 其他 → 抛业务异常
    private AlipayCloseResp handleFailure(AlipayCloseReq req, String subCode, String subMsg,
                                          String messageKey) {
        // 交易状态不支持关闭/撤销, 同步查询网关真实状态
        if (ACQ_TRADE_STATUS_ERROR.equals(subCode)) {
            AlipaySyncResp syncResp = syncStatus(req);
            if (TRADE_CLOSED.equals(syncResp.getTradeStatus())) {
                return toResp(req, syncResp.getCode(), subCode, subMsg);
            }
            log.error("支付宝订单无法关闭/撤销, 网关当前状态: tradeStatus={}", syncResp.getTradeStatus());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    messageKey, StrUtil.blankToDefault(subMsg, "交易状态不支持关闭"));
        }
        // 交易不存在, 视为已关闭成功
        if (ACQ_TRADE_NOT_EXIST.equals(subCode)) {
            return toResp(req, null, subCode, subMsg);
        }
        log.error("支付宝关闭/撤销失败: subCode={}, subMsg={}", subCode, subMsg);
        throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                messageKey, StrUtil.blankToDefault(subMsg, "关闭订单失败"));
    }

    /// 同步查询支付宝订单真实状态(关闭失败兜底)
    private AlipaySyncResp syncStatus(AlipayCloseReq req) {
        AlipaySyncReq syncReq = new AlipaySyncReq();
        syncReq.setOutTradeNo(req.getOutTradeNo());
        syncReq.setTradeNo(req.getTradeNo());
        syncReq.setCredential(req.getCredential());
        return alipaySyncService.sync(syncReq);
    }

    /// 组装关闭响应(订单号透传请求)
    private static AlipayCloseResp toResp(AlipayCloseReq req, String code, String subCode, String subMsg) {
        AlipayCloseResp resp = new AlipayCloseResp();
        resp.setOutTradeNo(req.getOutTradeNo());
        resp.setTradeNo(req.getTradeNo());
        resp.setCode(code);
        resp.setSubCode(subCode);
        resp.setSubMsg(subMsg);
        return resp;
    }
}
