package cn.daxpay.open.channel.ums.service.close;

import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import cn.daxpay.open.channel.ums.req.UmsCloseReq;
import cn.daxpay.open.channel.ums.resp.UmsCloseResp;
import cn.daxpay.open.channel.ums.sdk.UmsClient;
import cn.daxpay.open.channel.ums.util.UmsDateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/// # 银联商务通道关单服务
///
/// 扫码关单与 H5 关单接口不同, 通过 [UmsCloseReq.getMethod] 区分:
/// - **QRCODE**: 需要 qrCodeId(从支付返回的 billQRCode 链接末段提取)
/// - **其他 H5**: 需要 merOrderId(即商户订单号)
@Service
@Slf4j
@RequiredArgsConstructor
public class UmsCloseService {

    private final RestClient restClient;

    private static final String INST_MID_QR = "QRPAYDEFAULT";

    /// 关闭银联商务支付订单
    public UmsCloseResp close(UmsCloseReq req) {
        UmsClient client = new UmsClient(req.getCredential(), restClient);
        Map<String, Object> json = new HashMap<>();
        json.put("requestTimestamp", UmsDateUtil.nowDateTime());
        json.put("mid", req.getCredential().getMerchantNo());
        json.put("tid", req.getCredential().getTerminalNo());

        if (req.getMethod() == UmsPayMethod.QRCODE) {
            // 扫码关单
            json.put("qrCodeId", req.getQrCodeId());
            json.put("instMid", INST_MID_QR);
            json.put("attachRefund", true);
            client.closeQr(json);
        } else {
            // H5 关单
            json.put("merOrderId", req.getOutTradeNo());
            json.put("instMid", INST_MID_QR);
            client.closeH5(json);
        }
        log.info("银联商务关单成功: outTradeNo={}, method={}", req.getOutTradeNo(), req.getMethod());
        return new UmsCloseResp().setOutTradeNo(req.getOutTradeNo());
    }
}
