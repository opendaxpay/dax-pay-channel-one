package cn.daxpay.open.channel.alipay.service.callback;

import cn.daxpay.open.channel.alipay.config.AlipayAuthTypeEnum;
import cn.daxpay.open.channel.alipay.config.AlipaySdkCredential;
import cn.daxpay.open.channel.alipay.req.AlipayCallbackParseReq;
import cn.daxpay.open.channel.alipay.resp.AlipayCallbackParseResp;
import cn.hutool.core.util.StrUtil;
import com.alipay.api.internal.util.AlipaySignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

/// # 支付宝回调验签解析服务
///
/// 主应用接收到支付宝异步通知后, 将原始表单参数转发到本服务验签与解析。
/// 验签调用 [AlipaySignature], 公钥模式走 `rsaCheckV1`, 证书模式走 `rsaCertCheckV1`。
///
/// 支付回调字段: trade_status / out_trade_no / trade_no / gmt_payment / total_amount
/// 退款回调字段: refund_status / out_request_no / gmt_refund / refund_amount
///
/// 时间字段(gmt_payment / gmt_refund)为东八区本地时间字面量(无时区后缀),
/// 先用 LocalDateTime 接住再附加 +08:00 偏移, 落入实体的为 OffsetDateTime。
@Slf4j
@Service
public class AlipayCallbackParseService {

    /// 支付宝异步时间格式(东八区, 无时区后缀)
    private static final DateTimeFormatter CST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneOffset CST = ZoneOffset.ofHours(8);

    /// 解析支付回调
    public AlipayCallbackParseResp parsePay(AlipayCallbackParseReq req) {
        return doParse(req, false);
    }

    /// 解析退款回调
    public AlipayCallbackParseResp parseRefund(AlipayCallbackParseReq req) {
        return doParse(req, true);
    }

    /// 通用解析逻辑(支付/退款共用)
    private AlipayCallbackParseResp doParse(AlipayCallbackParseReq req, boolean refund) {
        AlipayCallbackParseResp resp = new AlipayCallbackParseResp()
                .setTradeType(refund ? "REFUND" : "PAY");
        Map<String, String> params = req.getParams();
        if (params == null || params.isEmpty()) {
            log.error("支付宝回调参数为空: refund={}", refund);
            return resp.setSuccess(false);
        }
        // 验签
        if (!verifySign(req.getCredential(), params)) {
            log.error("支付宝回调验签失败: refund={}", refund);
            return resp.setSuccess(false);
        }
        resp.setSuccess(true);

        if (refund) {
            // 退款回调: out_request_no 为主应用退款号, gmt_refund 为退款时间
            resp.setOutTradeNo(params.get("out_request_no"));
            resp.setOutRefundNo(params.get("trade_no"));
            String refundStatus = params.get("refund_status");
            resp.setTradeStatus(Objects.equals(refundStatus, "REFUND_SUCCESS") ? "SUCCESS" : "FAIL");
            resp.setAmount(yuanToFee(params.get("refund_amount")));
            resp.setFinishTime(parseCst(params.get("gmt_refund")));
        } else {
            // 支付回调
            resp.setOutTradeNo(params.get("out_trade_no"));
            resp.setTradeNo(params.get("trade_no"));
            String tradeStatus = params.get("trade_status");
            boolean paid = Objects.equals(tradeStatus, "TRADE_SUCCESS")
                    || Objects.equals(tradeStatus, "TRADE_FINISHED");
            resp.setTradeStatus(paid ? "SUCCESS" : "FAIL");
            resp.setAmount(yuanToFee(params.get("total_amount")));
            resp.setFinishTime(parseCst(params.get("gmt_payment")));
        }
        return resp;
    }

    /// 验证支付宝回调签名(公钥模式 rsaCheckV1 / 证书模式 rsaCertCheckV1)
    private boolean verifySign(AlipaySdkCredential credential, Map<String, String> params) {
        if (credential == null) {
            return false;
        }
        String signType = StrUtil.isBlank(credential.getSignType()) ? "RSA2" : credential.getSignType();
        try {
            if (AlipayAuthTypeEnum.fromCode(credential.getAuthType()).isCert()) {
                // 证书模式: rsaCertCheckV1 需要公钥证书文件路径, 凭证携带的是证书内容, 写临时文件后验签
                Path tempCert = writeTempCert(credential.getAlipayCert());
                try {
                    return AlipaySignature.rsaCertCheckV1(params, tempCert.toString(), "UTF-8", signType);
                } finally {
                    Files.deleteIfExists(tempCert);
                }
            }
            return AlipaySignature.rsaCheckV1(params, credential.getAlipayPublicKey(), "UTF-8", signType);
        } catch (Exception e) {
            log.error("支付宝回调验签异常", e);
            return false;
        }
    }

    /// 将证书内容写入临时文件(供 rsaCertCheckV1 读取)
    private Path writeTempCert(String certContent) throws Exception {
        Path temp = Files.createTempFile("alipay_public_cert_", ".crt");
        Files.writeString(temp, certContent, StandardCharsets.UTF_8);
        return temp;
    }

    /// 东八区本地时间字面量 → OffsetDateTime
    private OffsetDateTime parseCst(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, CST_FORMATTER).atOffset(CST);
        } catch (Exception e) {
            log.warn("支付宝回调时间解析失败: {}", text);
            return null;
        }
    }

    /// 元(字符串) → 分, 半角向上取整与平台 PayUtil 同口径
    private Long yuanToFee(String yuan) {
        if (StrUtil.isBlank(yuan)) {
            return null;
        }
        try {
            return new BigDecimal(yuan).multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        } catch (Exception e) {
            return null;
        }
    }
}
