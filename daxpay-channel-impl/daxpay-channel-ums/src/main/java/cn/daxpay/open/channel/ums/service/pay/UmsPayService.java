package cn.daxpay.open.channel.ums.service.pay;

import cn.daxpay.open.channel.ums.config.UmsSdkCredential;
import cn.daxpay.open.channel.ums.enums.UmsPayBodyType;
import cn.daxpay.open.channel.ums.enums.UmsPayMethod;
import cn.daxpay.open.channel.ums.req.UmsPayReq;
import cn.daxpay.open.channel.ums.resp.UmsPayResp;
import cn.daxpay.open.channel.ums.sdk.UmsClient;
import cn.daxpay.open.channel.ums.util.UmsDateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// # 银联商务通道支付下单服务
///
/// 按 [UmsPayReq.getMethod] 分发到对应支付方式:
/// - **QRCODE**: 扫码支付(POST JSON, 返回 billQRCode 二维码)
/// - **ALIPAY_H5 / WECHAT_H5 / WECHAT_CASHIER / UNION_JSAPI**: H5 支付(GET URL, 返回跳转链接)
///
/// H5 支付不是服务端直接调用银联, 而是生成带签名的跳转 URL, 由用户浏览器发起。
@Service
@Slf4j
@RequiredArgsConstructor
public class UmsPayService {

    private final RestClient restClient;

    /// 扫码业务类型码
    private static final String INST_MID_QR = "QRPAYDEFAULT";

    /// H5 业务类型码
    private static final String INST_MID_H5 = "H5DEFAULT";

    /// 通道支付下单
    public UmsPayResp pay(UmsPayReq req) {
        log.info("银联商务通道收到支付请求: outTradeNo={}, amount={}, method={}",
                req.getOutTradeNo(), req.getAmount(), req.getMethod());
        UmsPayMethod method = req.getMethod();
        if (Objects.isNull(method)) {
            throw new IllegalArgumentException("支付方式(method)不能为空");
        }
        return switch (method) {
            case QRCODE -> this.qrPay(req);
            case ALIPAY_H5 -> this.alipayH5(req);
            case WECHAT_H5 -> this.wechatH5(req);
            case WECHAT_CASHIER -> this.wechatCashier(req);
            case UNION_JSAPI -> this.unionH5(req);
        };
    }

    /// 扫码支付(主扫, 返回 billQRCode 二维码链接)
    private UmsPayResp qrPay(UmsPayReq req) {
        UmsClient client = new UmsClient(req.getCredential(), restClient);
        Map<String, Object> json = this.buildQrBaseParam(req);
        // 商品名称
        if (StrUtil.isNotBlank(req.getDescription())) {
            Map<String, Object> goods = new HashMap<>();
            goods.put("goodsName", req.getDescription());
            json.put("goods", List.of(goods));
        }
        JSONObject response = client.qrPay(json);
        String billQRCode = response.getStr("billQRCode");
        if (StrUtil.isBlank(billQRCode)) {
            throw new IllegalStateException("银联商务扫码支付未返回 billQRCode");
        }
        return new UmsPayResp()
                .setOutTradeNo(req.getOutTradeNo())
                .setPayBody(billQRCode)
                .setPayBodyType(UmsPayBodyType.QR_CODE);
    }

    /// 支付宝 H5 支付(返回跳转链接)
    private UmsPayResp alipayH5(UmsPayReq req) {
        UmsClient client = new UmsClient(req.getCredential(), restClient);
        Map<String, Object> json = this.buildH5BaseParam(req);
        String url = client.alipayH5(json);
        return new UmsPayResp()
                .setOutTradeNo(req.getOutTradeNo())
                .setPayBody(url)
                .setPayBodyType(UmsPayBodyType.LINK);
    }

    /// 微信 H5 支付(返回跳转链接)
    private UmsPayResp wechatH5(UmsPayReq req) {
        UmsClient client = new UmsClient(req.getCredential(), restClient);
        Map<String, Object> json = this.buildH5BaseParam(req);
        // 微信 H5 场景信息
        json.put("sceneType", "AND_WAP");
        json.put("merAppName", req.getDescription());
        json.put("merAppId", req.getClientIp());
        String url = client.wechatH5(json);
        return new UmsPayResp()
                .setOutTradeNo(req.getOutTradeNo())
                .setPayBody(url)
                .setPayBodyType(UmsPayBodyType.LINK);
    }

    /// 微信小程序收银台支付(H5 转小程序, 返回跳转链接)
    private UmsPayResp wechatCashier(UmsPayReq req) {
        if (StrUtil.isBlank(req.getWxAppId())) {
            throw new IllegalArgumentException("微信小程序收银台支付需传入 wxAppId");
        }
        UmsClient client = new UmsClient(req.getCredential(), restClient);
        Map<String, Object> json = this.buildH5BaseParam(req);
        json.put("subAppId", req.getWxAppId());
        String url = client.wechatH5ToMini(json);
        return new UmsPayResp()
                .setOutTradeNo(req.getOutTradeNo())
                .setPayBody(url)
                .setPayBodyType(UmsPayBodyType.LINK);
    }

    /// 银联云闪付 H5 支付(返回跳转链接)
    private UmsPayResp unionH5(UmsPayReq req) {
        UmsClient client = new UmsClient(req.getCredential(), restClient);
        Map<String, Object> json = this.buildH5BaseParam(req);
        String url = client.unionH5(json);
        return new UmsPayResp()
                .setOutTradeNo(req.getOutTradeNo())
                .setPayBody(url)
                .setPayBodyType(UmsPayBodyType.LINK);
    }

    /// 构建扫码请求公共参数(mid/tid/instMid/billNo/totalAmount/notifyUrl)
    private Map<String, Object> buildQrBaseParam(UmsPayReq req) {
        UmsSdkCredential cred = req.getCredential();
        Map<String, Object> json = new HashMap<>();
        json.put("requestTimestamp", UmsDateUtil.nowDateTime());
        json.put("mid", cred.getMerchantNo());
        json.put("tid", cred.getTerminalNo());
        json.put("instMid", INST_MID_QR);
        json.put("billNo", req.getOutTradeNo());
        json.put("billDate", UmsDateUtil.todayDate());
        json.put("totalAmount", req.getAmount());
        json.put("notifyUrl", req.getNotifyUrl());
        if (Boolean.TRUE.equals(req.getLimitCreditCard())) {
            json.put("limitCreditCard", "true");
        }
        return json;
    }

    /// 构建请求公共参数(mid/tid/instMid/merOrderId/totalAmount/notifyUrl)
    private Map<String, Object> buildH5BaseParam(UmsPayReq req) {
        UmsSdkCredential cred = req.getCredential();
        Map<String, Object> json = new HashMap<>();
        json.put("requestTimestamp", UmsDateUtil.nowDateTime());
        json.put("mid", cred.getMerchantNo());
        json.put("tid", cred.getTerminalNo());
        json.put("instMid", INST_MID_H5);
        json.put("merOrderId", req.getOutTradeNo());
        json.put("totalAmount", req.getAmount());
        json.put("notifyUrl", req.getNotifyUrl());
        if (Boolean.TRUE.equals(req.getLimitCreditCard())) {
            json.put("limitCreditCard", "true");
        }
        return json;
    }
}
