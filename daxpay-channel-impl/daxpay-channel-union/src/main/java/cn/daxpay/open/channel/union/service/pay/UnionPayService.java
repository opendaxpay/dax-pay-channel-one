package cn.daxpay.open.channel.union.service.pay;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.enums.UnionPayBodyType;
import cn.daxpay.open.channel.union.enums.UnionPayMethod;
import cn.daxpay.open.channel.union.req.UnionPayReq;
import cn.daxpay.open.channel.union.resp.UnionPayResp;
import cn.daxpay.open.channel.union.sdk.UnionClient;
import cn.daxpay.open.channel.union.util.UnionDateUtil;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/// # 云闪付通道支付下单服务
///
/// 按 [UnionPayReq.getMethod] 分发到对应支付方式:
/// - **QRCODE**: 主扫(申请二维码, 返回 qrCode, 前端渲染二维码)
/// - **BARCODE**: 被扫(付款码消费, 同步返回结果, 主应用走 sync 确认)
/// - **H5**: WAP 网关支付(返回自动提交 HTML form, 浏览器跳转银联收银台)
@Service
@Slf4j
@RequiredArgsConstructor
public class UnionPayService {

    private final RestClient restClient;

    /// 银联接口版本
    private static final String VERSION = "5.1.0";

    /// 编码方式
    private static final String ENCODING = "UTF-8";

    /// 接入类型(0=直连商户)
    private static final String ACCESS_TYPE = "0";

    /// 交易币种(156=人民币)
    private static final String CURRENCY_CODE = "156";

    /// 手机渠道
    private static final String CHANNEL_TYPE_MOBILE = "08";

    /// 通道支付下单
    public UnionPayResp pay(UnionPayReq req) {
        log.info("云闪付通道收到支付请求: outTradeNo={}, amount={}, method={}",
                req.getOutTradeNo(), req.getAmount(), req.getMethod());
        UnionPayMethod method = req.getMethod();
        if (method == null) {
            throw new IllegalArgumentException("支付方式(method)不能为空");
        }
        return switch (method) {
            case QRCODE -> this.applyQrCode(req);
            case BARCODE -> this.consume(req);
            case H5 -> this.wapPay(req);
        };
    }

    /// 主扫支付(申请二维码, 返回 qrCode 二维码内容)
    private UnionPayResp applyQrCode(UnionPayReq req) {
        UnionClient client = new UnionClient(req.getCredential(), restClient);
        Map<String, Object> param = this.buildCommonParam(req.getCredential());
        param.put("orderId", req.getOutTradeNo());
        param.put("txnAmt", req.getAmount());
        param.put("channelType", CHANNEL_TYPE_MOBILE);
        param.put("backUrl", req.getNotifyUrl());
        Map<String, String> response = client.applyQrCode(param);
        this.checkSuccess(response);

        // ACP 主扫申请二维码标准响应字段为 qrCode；兼容部分历史实现使用的 qrNo。
        String qrCode = response.get("qrCode");
        if (StrUtil.isBlank(qrCode)) {
            qrCode = response.get("qrNo");
        }
        if (StrUtil.isBlank(qrCode)) {
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.unionRequestFailed", "银联响应成功但未返回 qrCode");
        }
        return new UnionPayResp()
                .setOutTradeNo(req.getOutTradeNo())
                .setPayBody(qrCode)
                .setPayBodyType(UnionPayBodyType.QR_CODE);
    }

    /// 被扫支付(付款码消费, 同步返回支付结果)
    ///
    /// respCode=00 支付成功 / 03 处理中, 其他视为失败抛异常。
    /// 被扫无支付内容(payBody 为空), 主应用通过 sync/回调确认最终状态。
    private UnionPayResp consume(UnionPayReq req) {
        if (StrUtil.isBlank(req.getAuthCode())) {
            throw new IllegalArgumentException("被扫支付需传入付款码 authCode");
        }
        UnionClient client = new UnionClient(req.getCredential(), restClient);
        Map<String, Object> param = this.buildCommonParam(req.getCredential());
        param.put("orderId", req.getOutTradeNo());
        param.put("txnAmt", req.getAmount());
        param.put("qrNo", req.getAuthCode());
        Map<String, String> response = client.consume(param);
        String respCode = response.get("respCode");
        if (!"00".equals(respCode) && !"03".equals(respCode)) {
            throwUnionError(response);
        }
        return new UnionPayResp().setOutTradeNo(req.getOutTradeNo());
    }

    /// H5/WAP 网关支付(返回自动提交 HTML form)
    private UnionPayResp wapPay(UnionPayReq req) {
        UnionClient client = new UnionClient(req.getCredential(), restClient);
        Map<String, Object> param = this.buildCommonParam(req.getCredential());
        param.put("orderId", req.getOutTradeNo());
        param.put("txnAmt", req.getAmount());
        if (StrUtil.isNotBlank(req.getDescription())) {
            param.put("orderDesc", req.getDescription());
        }
        param.put("backUrl", req.getNotifyUrl());
        String html = client.buildWapFormHtml(param);
        return new UnionPayResp()
                .setOutTradeNo(req.getOutTradeNo())
                .setPayBody(html)
                .setPayBodyType(UnionPayBodyType.LINK);
    }

    /// 构建银联公共参数(version/encoding/merId/txnTime/accessType/currencyCode)
    private Map<String, Object> buildCommonParam(UnionSdkCredential cred) {
        Map<String, Object> param = new HashMap<>();
        param.put("version", VERSION);
        param.put("encoding", ENCODING);
        param.put("merId", cred.getMerId());
        param.put("txnTime", UnionDateUtil.txnTime());
        param.put("accessType", ACCESS_TYPE);
        param.put("currencyCode", CURRENCY_CODE);
        return param;
    }

    /// 后台同步接口成功码校验。
    private void checkSuccess(Map<String, String> response) {
        if (!"00".equals(response.get("respCode"))) {
            throwUnionError(response);
        }
    }

    /// 保留银联原始业务错误，避免被“缺少结果字段”等二次错误覆盖。
    private void throwUnionError(Map<String, String> response) {
        String respCode = response.get("respCode");
        String respMsg = response.get("respMsg");
        String message = StrUtil.isBlank(respMsg)
                ? StrUtil.format("银联交易失败(respCode={})", respCode)
                : StrUtil.format("{} (respCode={})", respMsg, respCode);
        throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                "channel.error.unionRequestFailed", message);
    }
}
