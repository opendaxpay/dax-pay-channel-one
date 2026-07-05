package cn.daxpay.open.channel.ums.service.callback;

import cn.daxpay.open.channel.ums.req.UmsCallbackParseReq;
import cn.daxpay.open.channel.ums.resp.UmsCallbackParseResp;
import cn.daxpay.open.channel.ums.util.UmsSignUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/// # 银联商务回调验签解析服务
///
/// 主应用接收到银联商务异步通知后, 将回调参数连同通道凭证转发到本服务,
/// 使用 [UmsSignUtil] 完成 MD5/SHA256 验签, 然后解析为结构化业务数据返回主应用。
///
/// 与抖音回调(header+body 密文)不同, 银联商务回调为 form 参数(Map), 含 sign/signType,
/// 验签方式为字典序拼接 + MD5/SHA256(secretKey), 无需平台证书。
///
/// 回调格式自动判断:
/// - **扫码回调**: 含 `billNo` 字段, 状态字段为 `billStatus`, 明细在嵌套 `billPayment` 中
/// - **H5 回调**: 含 `merOrderId` 字段, 状态字段为 `status`, 字段扁平无嵌套
@Service
@Slf4j
public class UmsCallbackParseService {

    /// 解析支付回调(验签 + 提取支付业务字段)
    public UmsCallbackParseResp parsePay(UmsCallbackParseReq req) {
        Map<String, String> params = req.getParams();
        boolean verified = UmsSignUtil.verifyCallback(params, req.getCredential().getSecretKey());
        if (!verified) {
            log.error("银联商务支付回调验签失败");
            return new UmsCallbackParseResp().setVerified(false);
        }
        // 通过字段名判断扫码回调还是 H5 回调
        if (params.containsKey("billNo")) {
            return this.parseQrPayCallback(params);
        } else {
            return this.parseH5PayCallback(params);
        }
    }

    /// 解析退款回调(验签 + 提取退款业务字段)
    public UmsCallbackParseResp parseRefund(UmsCallbackParseReq req) {
        Map<String, String> params = req.getParams();
        boolean verified = UmsSignUtil.verifyCallback(params, req.getCredential().getSecretKey());
        if (!verified) {
            log.error("银联商务退款回调验签失败");
            return new UmsCallbackParseResp().setVerified(false);
        }
        // 退款回调主要为 H5 模式, 字段包含 refundOrderId
        return this.parseH5RefundCallback(params);
    }

    /// 解析扫码支付回调
    ///
    /// 状态字段 billStatus: PAID/REFUND(成功) / UNPAID(进行中) / CLOSED(关闭)
    /// 明细在嵌套 billPayment 中(JSON 字符串): payTime/buyerId/targetSys/targetOrderId
    private UmsCallbackParseResp parseQrPayCallback(Map<String, String> params) {
        JSONObject param = new JSONObject(params);
        UmsCallbackParseResp resp = new UmsCallbackParseResp()
                .setVerified(true)
                .setTradeType("PAY")
                .setOutTradeNo(param.getStr("billNo"));
        // 支付状态
        String billStatus = param.getStr("billStatus");
        resp.setTradeStatus(this.mapPayStatus(billStatus));
        // 金额
        resp.setAmount(param.getLong("totalAmount"));
        resp.setRealAmount(param.getLong("receiptAmount"));
        // 扫码明细在 billPayment 嵌套对象中
        JSONObject billPayment = param.getJSONObject("billPayment");
        if (billPayment != null) {
            resp.setFinishTime(billPayment.getStr("payTime"));
            resp.setBuyerId(billPayment.getStr("buyerId"));
            resp.setTargetSys(billPayment.getStr("targetSys"));
            resp.setTargetOrderId(billPayment.getStr("targetOrderId"));
        }
        return resp;
    }

    /// 解析 H5 支付回调
    ///
    /// 状态字段 status: TRADE_SUCCESS(成功) / UNPAID(进行中) / TRADE_CLOSED(关闭)
    private UmsCallbackParseResp parseH5PayCallback(Map<String, String> params) {
        JSONObject param = new JSONObject(params);
        UmsCallbackParseResp resp = new UmsCallbackParseResp()
                .setVerified(true)
                .setTradeType("PAY")
                .setOutTradeNo(param.getStr("merOrderId"));
        // 支付状态
        String status = param.getStr("status");
        resp.setTradeStatus(this.mapH5PayStatus(status));
        // 金额(扁平字段)
        resp.setAmount(param.getLong("totalAmount"));
        resp.setRealAmount(param.getLong("receiptAmount"));
        resp.setFinishTime(param.getStr("payTime"));
        resp.setBuyerId(param.getStr("buyerId"));
        resp.setTargetSys(param.getStr("targetSys"));
        resp.setTargetOrderId(param.getStr("targetOrderId"));
        return resp;
    }

    /// 解析 H5 退款回调
    ///
    /// 状态字段 status: TRADE_REFUND(退款成功) / TRADE_CLOSED(关闭)
    private UmsCallbackParseResp parseH5RefundCallback(Map<String, String> params) {
        JSONObject param = new JSONObject(params);
        UmsCallbackParseResp resp = new UmsCallbackParseResp()
                .setVerified(true)
                .setTradeType("REFUND")
                .setOutRefundNo(param.getStr("refundOrderId"));
        // 退款状态
        String status = param.getStr("status");
        resp.setTradeStatus(this.mapRefundStatus(status));
        // 退款金额
        resp.setAmount(param.getLong("refundAmount"));
        resp.setFinishTime(param.getStr("refundPayTime"));
        resp.setTargetSys(param.getStr("targetSys"));
        resp.setTargetOrderId(param.getStr("refundTargetOrderId"));
        return resp;
    }

    /// 扫码支付状态映射: PAID/REFUND→SUCCESS, UNPAID→PROGRESS, CLOSED→CLOSED
    private String mapPayStatus(String billStatus) {
        if (StrUtil.isBlank(billStatus)) {
            return "PROGRESS";
        }
        return switch (billStatus) {
            case "PAID", "REFUND" -> "SUCCESS";
            case "CLOSED" -> "CLOSED";
            default -> "PROGRESS";
        };
    }

    /// H5 支付状态映射: TRADE_SUCCESS→SUCCESS, UNPAID→PROGRESS, TRADE_CLOSED→CLOSED
    private String mapH5PayStatus(String status) {
        if (StrUtil.isBlank(status)) {
            return "PROGRESS";
        }
        return switch (status) {
            case "TRADE_SUCCESS" -> "SUCCESS";
            case "TRADE_CLOSED" -> "CLOSED";
            default -> "PROGRESS";
        };
    }

    /// 退款状态映射: TRADE_REFUND→SUCCESS, TRADE_CLOSED→CLOSED
    private String mapRefundStatus(String status) {
        if (StrUtil.isBlank(status)) {
            return "PROGRESS";
        }
        return switch (status) {
            case "TRADE_REFUND" -> "SUCCESS";
            case "TRADE_CLOSED" -> "CLOSED";
            default -> "PROGRESS";
        };
    }
}
