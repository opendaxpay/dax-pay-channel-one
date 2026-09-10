package cn.daxpay.open.channel.ums.service.refund;

import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import cn.daxpay.open.channel.ums.req.UmsRefundReq;
import cn.daxpay.open.channel.ums.resp.UmsRefundResp;
import cn.daxpay.open.channel.ums.sdk.UmsClient;
import cn.daxpay.open.channel.ums.util.UmsDateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/// # 银联商务通道退款服务
///
/// 扫码退款与 H5 退款接口不同, 通过 [UmsRefundReq.getMethod] 区分。
/// 退款状态: SUCCESS(成功) / PROCESSING(处理中) / FAIL(失败)。
@Service
@Slf4j
@RequiredArgsConstructor
public class UmsRefundService {

    private final RestClient restClient;

    private static final String INST_MID_QR = "QRPAYDEFAULT";
    private static final String INST_MID_H5 = "H5DEFAULT";

    /// 发起银联商务退款
    public UmsRefundResp refund(UmsRefundReq req) {
        UmsClient client = new UmsClient(req.getCredential(), restClient);
        Map<String, Object> json = new HashMap<>();
        json.put("requestTimestamp", UmsDateUtil.nowDateTime());
        json.put("mid", req.getCredential().getMerchantNo());
        json.put("tid", req.getCredential().getTerminalNo());
        json.put("refundOrderId", req.getOutRefundNo());
        json.put("refundAmount", req.getRefundAmount());
        if (StrUtil.isNotBlank(req.getReason())) {
            json.put("refundDesc", req.getReason());
        }

        JSONObject response;
        String finishTimeField;
        if (req.getMethod() == UmsPayMethod.QRCODE) {
            // 扫码退款
            json.put("billNo", req.getOutTradeNo());
            // billDate 由主应用以 UTC OffsetDateTime 传入, 按银联商务东八区转换
            if (Objects.nonNull(req.getBillDate())) {
                json.put("billDate", UmsDateUtil.formatCstDate(req.getBillDate()));
            }
            json.put("instMid", INST_MID_QR);
            response = client.refundQr(json);
            finishTimeField = "refundPayTime";
        } else {
            // H5 退款
            json.put("merOrderId", req.getOutTradeNo());
            json.put("instMid", INST_MID_H5);
            response = client.refundH5(json);
            finishTimeField = "payTime";
        }

        String refundStatus = response.getStr("refundStatus");
        String finishTime = response.getStr(finishTimeField);
        log.info("银联商务退款: outRefundNo={}, refundStatus={}", req.getOutRefundNo(), refundStatus);
        return new UmsRefundResp()
                .setOutRefundNo(req.getOutRefundNo())
                .setRefundStatus(refundStatus)
                .setFinishTime(finishTime);
    }
}
