package cn.daxpay.open.channel.union.service.callback;

import cn.daxpay.open.channel.union.req.UnionCallbackParseReq;
import cn.daxpay.open.channel.union.resp.UnionCallbackParseResp;
import cn.daxpay.open.channel.union.util.UnionSignUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # 云闪付回调验签解析服务
///
/// 主应用接收到银联异步通知后, 将回调参数连同通道凭证转发到本服务,
/// 使用 [UnionSignUtil] 完成 RSA2 证书验签(银联回调附带 signPubKeyCert), 然后解析为结构化业务数据返回主应用。
///
/// 银联回调为 form 参数(Map), 通过 txnType 区分:
/// - **txnType=01**: 支付回调(orderId=商户订单号)
/// - **txnType=04**: 退款回调(orderId=退款单号)
///
/// 回调状态字段 respCode: 00 成功, 其他需忽略(银联会重试)。
@Service
@Slf4j
public class UnionCallbackParseService {

    /// 解析支付回调(验签 + 提取支付业务字段)
    ///
    /// 支付回调与退款回调共用一个入口, 内部按 txnType 自动分发。
    public UnionCallbackParseResp parsePay(UnionCallbackParseReq req) {
        Map<String, String> params = req.getParams();
        boolean verified = UnionSignUtil.verifyCallback(params, req.getCredential());
        if (!verified) {
            log.error("云闪付支付回调验签失败");
            return new UnionCallbackParseResp().setVerified(false);
        }
        String txnType = params.get("txnType");
        // 退款回调(txnType=04)也可能进入此入口, 按类型分发
        if ("04".equals(txnType)) {
            return this.parseRefundCallback(params);
        }
        return this.parsePayCallback(params);
    }

    /// 解析退款回调(验签 + 提取退款业务字段)
    public UnionCallbackParseResp parseRefund(UnionCallbackParseReq req) {
        Map<String, String> params = req.getParams();
        boolean verified = UnionSignUtil.verifyCallback(params, req.getCredential());
        if (!verified) {
            log.error("云闪付退款回调验签失败");
            return new UnionCallbackParseResp().setVerified(false);
        }
        return this.parseRefundCallback(params);
    }

    /// 解析支付回调
    ///
    /// 关键字段: orderId(商户订单号) / txnAmt(金额) / queryId(交易凭证, 退款必填) / txnTime(完成时间)
    private UnionCallbackParseResp parsePayCallback(Map<String, String> params) {
        JSONObject param = new JSONObject(params);
        return new UnionCallbackParseResp()
                .setVerified(true)
                .setTradeType("PAY")
                .setOutTradeNo(param.getStr("orderId"))
                .setTradeStatus(this.mapStatus(param.getStr("respCode")))
                .setAmount(param.getLong("txnAmt"))
                .setRealAmount(param.getLong("settleAmt"))
                .setFinishTime(param.getStr("txnTime"))
                .setQueryId(param.getStr("queryId"))
                .setBuyerId(param.getStr("accNo"));
    }

    /// 解析退款回调
    private UnionCallbackParseResp parseRefundCallback(Map<String, String> params) {
        JSONObject param = new JSONObject(params);
        return new UnionCallbackParseResp()
                .setVerified(true)
                .setTradeType("REFUND")
                .setOutRefundNo(param.getStr("orderId"))
                .setTradeStatus(this.mapStatus(param.getStr("respCode")))
                .setAmount(param.getLong("txnAmt"))
                .setFinishTime(param.getStr("txnTime"));
    }

    /// 回调状态映射: respCode 00→SUCCESS, 其他→PROGRESS(等待重试或同步确认)
    private String mapStatus(String respCode) {
        if (StrUtil.isBlank(respCode)) {
            return "PROGRESS";
        }
        return "00".equals(respCode) ? "SUCCESS" : "PROGRESS";
    }
}
