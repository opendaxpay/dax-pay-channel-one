package cn.daxpay.open.channel.ums.service.sync;

import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import cn.daxpay.open.channel.ums.req.UmsSyncReq;
import cn.daxpay.open.channel.ums.resp.UmsSyncResp;
import cn.daxpay.open.channel.ums.sdk.UmsClient;
import cn.daxpay.open.channel.ums.util.UmsDateUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/// # 银联商务通道支付同步服务
///
/// 查询支付订单最终状态。扫码与 H5 查询接口不同, 通过 [UmsSyncReq.getMethod] 区分。
/// 子应用将两种不同的原始状态码统一映射为平台标准 tradeStatus:
/// - **SUCCESS**: 扫码 PAID/REFUND, H5 TRADE_SUCCESS
/// - **PROGRESS**: UNPAID
/// - **CLOSED**: 扫码 CLOSED, H5 TRADE_CLOSED
@Service
@Slf4j
@RequiredArgsConstructor
public class UmsSyncService {

    private final RestClient restClient;

    private static final String INST_MID_QR = "QRPAYDEFAULT";

    /// 同步银联商务支付订单状态
    public UmsSyncResp sync(UmsSyncReq req) {
        UmsClient client = new UmsClient(req.getCredential(), restClient);
        Map<String, Object> json = new HashMap<>();
        json.put("requestTimestamp", UmsDateUtil.nowDateTime());
        json.put("mid", req.getCredential().getMerchantNo());
        json.put("tid", req.getCredential().getTerminalNo());
        json.put("instMid", INST_MID_QR);

        if (req.getMethod() == UmsPayMethod.QRCODE) {
            json.put("billNo", req.getOutTradeNo());
            // billDate 由主应用以 UTC OffsetDateTime 传入, 按银联商务东八区转换
            if (req.getBillDate() != null) {
                json.put("billDate", UmsDateUtil.formatCstDate(req.getBillDate()));
            }
            JSONObject response = client.queryQrOrder(json);
            return this.parseQrResp(req.getOutTradeNo(), response);
        } else {
            json.put("merOrderId", req.getOutTradeNo());
            JSONObject response = client.queryH5Order(json);
            return this.parseH5Resp(req.getOutTradeNo(), response);
        }
    }

    /// 解析扫码查询响应(billStatus + billPayment 嵌套对象)
    private UmsSyncResp parseQrResp(String outTradeNo, JSONObject response) {
        String billStatus = response.getStr("billStatus");
        UmsSyncResp resp = new UmsSyncResp().setOutTradeNo(outTradeNo);
        switch (billStatus) {
            case "PAID", "REFUND" -> {
                resp.setTradeStatus("SUCCESS");
                resp.setTotalAmount(response.getLong("totalAmount"));
                // 扫码支付明细在 billPayment 嵌套对象中
                JSONObject billPayment = response.getJSONObject("billPayment");
                if (billPayment != null) {
                    resp.setRealAmount(billPayment.getLong("buyerPayAmount"));
                    resp.setPayTime(billPayment.getStr("payTime"));
                    resp.setBuyerId(billPayment.getStr("buyerId"));
                    resp.setTargetSys(billPayment.getStr("targetSys"));
                    resp.setTargetOrderId(billPayment.getStr("targetOrderId"));
                }
            }
            case "CLOSED" -> resp.setTradeStatus("CLOSED");
            case null, default -> resp.setTradeStatus("PROGRESS");
        }
        return resp;
    }

    /// 解析 H5 查询响应(status 字段直接取, 无嵌套)
    private UmsSyncResp parseH5Resp(String outTradeNo, JSONObject response) {
        String status = response.getStr("status");
        UmsSyncResp resp = new UmsSyncResp().setOutTradeNo(outTradeNo);
        switch (status) {
            case "TRADE_SUCCESS" -> {
                resp.setTradeStatus("SUCCESS");
                resp.setTotalAmount(response.getLong("totalAmount"));
                resp.setRealAmount(response.getLong("buyerPayAmount"));
                resp.setPayTime(response.getStr("payTime"));
                resp.setBuyerId(response.getStr("buyerId"));
                resp.setTargetSys(response.getStr("targetSys"));
                resp.setTargetOrderId(response.getStr("targetOrderId"));
            }
            case "TRADE_CLOSED" -> resp.setTradeStatus("CLOSED");
            case null, default -> resp.setTradeStatus("PROGRESS");
        }
        return resp;
    }
}
