package cn.daxpay.open.channel.ums.service.refund;

import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import cn.daxpay.open.channel.ums.req.UmsRefundSyncReq;
import cn.daxpay.open.channel.ums.resp.UmsRefundSyncResp;
import cn.daxpay.open.channel.ums.sdk.UmsClient;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/// # 银联商务通道退款同步服务
///
/// 通过退款单号查询退款最终状态。扫码与 H5 查询接口不同:
/// - **扫码**: 复用订单查询接口(bills/query), 额外传 refundOrderId, 响应中取 refundBillPayment
/// - **H5**: 使用退款查询接口(refund-query), 响应中取 refundStatus
///
/// 统一状态码: SUCCESS / PROGRESS / CLOSED
@Service
@Slf4j
public class UmsRefundSyncService {

    private static final String INST_MID_QR = "QRPAYDEFAULT";
    private static final String INST_MID_H5 = "H5DEFAULT";

    /// 同步银联商务退款状态
    public UmsRefundSyncResp sync(UmsRefundSyncReq req) {
        UmsClient client = new UmsClient(req.getCredential());
        Map<String, Object> json = new HashMap<>();
        json.put("requestTimestamp", DateUtil.formatDateTime(new Date()));
        json.put("mid", req.getCredential().getMerchantNo());
        json.put("tid", req.getCredential().getTerminalNo());

        if (req.getMethod() == UmsPayMethod.QRCODE) {
            // 扫码退款查询: 复用 bills/query 接口
            json.put("instMid", INST_MID_QR);
            json.put("billNo", req.getOutTradeNo());
            json.put("refundOrderId", req.getOutRefundNo());
            if (req.getBillDate() != null) {
                json.put("billDate", req.getBillDate());
            }
            JSONObject response = client.queryQrOrder(json);
            return this.parseQrResp(req.getOutRefundNo(), response);
        } else {
            // H5 退款查询
            json.put("instMid", INST_MID_H5);
            json.put("merOrderId", req.getOutRefundNo());
            JSONObject response = client.queryH5Refund(json);
            return this.parseH5Resp(req.getOutRefundNo(), response);
        }
    }

    /// 解析扫码退款查询响应(refundBillPayment 嵌套对象)
    private UmsRefundSyncResp parseQrResp(String outRefundNo, JSONObject response) {
        UmsRefundSyncResp resp = new UmsRefundSyncResp().setOutRefundNo(outRefundNo);
        JSONObject refundBillPayment = response.getJSONObject("refundBillPayment");
        if (refundBillPayment != null) {
            String status = refundBillPayment.getStr("status");
            switch (status) {
                case "TRADE_SUCCESS" -> resp.setRefundStatus("SUCCESS");
                case "TRADE_REFUND" -> resp.setRefundStatus("CLOSED");
                case null, default -> resp.setRefundStatus("PROGRESS");
            }
            resp.setRefundAmount(refundBillPayment.getLong("totalAmount"));
            resp.setFinishTime(refundBillPayment.getStr("payTime"));
        } else {
            resp.setRefundStatus("PROGRESS");
        }
        return resp;
    }

    /// 解析 H5 退款查询响应(refundStatus 字段)
    private UmsRefundSyncResp parseH5Resp(String outRefundNo, JSONObject response) {
        UmsRefundSyncResp resp = new UmsRefundSyncResp().setOutRefundNo(outRefundNo);
        String status = response.getStr("refundStatus");
        switch (status) {
            case "SUCCESS" -> {
                resp.setRefundStatus("SUCCESS");
                resp.setRefundAmount(response.getLong("totalAmount"));
                resp.setFinishTime(response.getStr("payTime"));
            }
            case "FAIL" -> resp.setRefundStatus("CLOSED");
            case null, default -> resp.setRefundStatus("PROGRESS");
        }
        return resp;
    }
}
