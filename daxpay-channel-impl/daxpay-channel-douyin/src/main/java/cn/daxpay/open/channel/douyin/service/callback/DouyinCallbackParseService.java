package cn.daxpay.open.channel.douyin.service.callback;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.req.DouyinCallbackParseReq;
import cn.daxpay.open.channel.douyin.resp.DouyinCallbackParseResp;
import cn.daxpay.open.channel.douyin.resp.DouyinTransferCallbackParseResp;
import cn.hutool.core.util.StrUtil;
import com.douyinpay.api.notification.RequestParam;
import com.douyinpay.api.payments.common.ApiTransaction;
import com.douyinpay.api.refund.model.ApiRefund;
import com.douyinpay.api.transfer.models.TransferPayeeNotification;
import com.douyinpay.define.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 抖音回调验签解析服务
///
/// 主应用接收到抖音异步通知后, 将原始 header + body + 凭证转发到本服务,
/// 使用 [com.douyinpay.api.notification.NotificationParser] 完成平台证书验签与 AES 解密。
///
/// 区分支付回调([#parsePay])与退款回调([#parseRefund]), 分别解析为
/// [ApiTransaction] / [ApiRefund]。
@Slf4j
@Service
public class DouyinCallbackParseService {

    /// 解析支付回调(验签 + 解密为 ApiTransaction)
    public DouyinCallbackParseResp parsePay(DouyinCallbackParseReq req) {
        try {
            RequestParam requestParam = buildRequestParam(req);
            ApiTransaction transaction = DouyinSdkConfig.buildNotificationParser(req.getCredential())
                    .parse(requestParam, ApiTransaction.class);
            DouyinCallbackParseResp resp = new DouyinCallbackParseResp()
                    .setVerified(true)
                    .setTradeType("PAY")
                    .setOutTradeNo(transaction.getOutTradeNo())
                    .setTransactionId(transaction.getTransactionId())
                    .setTradeState(transaction.getTradeState())
                    .setSuccessTime(transaction.getSuccessTime());
            if (transaction.getAmount() != null) {
                resp.setAmount(transaction.getAmount().getTotal().longValue());
            }
            if (transaction.getPayer() != null) {
                resp.setOpenid(transaction.getPayer().getOpenid());
            }
            return resp;
        } catch (Exception e) {
            log.error("抖音支付回调验签解析失败", e);
            return new DouyinCallbackParseResp().setVerified(false);
        }
    }

    /// 解析退款回调(验签 + 解密为 ApiRefund)
    public DouyinCallbackParseResp parseRefund(DouyinCallbackParseReq req) {
        try {
            RequestParam requestParam = buildRequestParam(req);
            ApiRefund refund = DouyinSdkConfig.buildNotificationParser(req.getCredential())
                    .parse(requestParam, ApiRefund.class);
            DouyinCallbackParseResp resp = new DouyinCallbackParseResp()
                    .setVerified(true)
                    .setTradeType("REFUND")
                    .setOutTradeNo(refund.getOutTradeNo())
                    .setOutRefundNo(refund.getOutRefundNo())
                    .setRefundId(refund.getRefundId())
                    .setRefundStatus(refund.getRefundStatus())
                    .setSuccessTime(refund.getSuccessTime());
            if (refund.getAmount() != null) {
                resp.setAmount(refund.getAmount().getRefund().longValue());
            }
            return resp;
        } catch (Exception e) {
            log.error("抖音退款回调验签解析失败", e);
            return new DouyinCallbackParseResp().setVerified(false);
        }
    }

    /// 解析转账回调(验签 + 解密为 TransferPayeeNotification)
    ///
    /// 抖音商家转账异步通知, 通知体仅含 order_id(通道转账单号), 不含商户单号 out_bill_no。
    public DouyinTransferCallbackParseResp parseTransfer(DouyinCallbackParseReq req) {
        try {
            RequestParam requestParam = buildRequestParam(req);
            TransferPayeeNotification notification = DouyinSdkConfig.buildNotificationParser(req.getCredential())
                    .parse(requestParam, TransferPayeeNotification.class);
            return new DouyinTransferCallbackParseResp()
                    .setVerified(true)
                    .setTransferBillNo(notification.getOrderId())
                    .setTransferState(notification.getStatus())
                    .setTransferStatusDesc(notification.getStatusDesc())
                    .setSuccessTime(notification.getSuccessTime());
        } catch (Exception e) {
            log.error("抖音转账回调验签解析失败", e);
            return new DouyinTransferCallbackParseResp().setVerified(false);
        }
    }

    /// 组装 SDK 验签所需的 RequestParam
    private RequestParam buildRequestParam(DouyinCallbackParseReq req) {
        return new RequestParam.Builder()
                .serialNumber(req.getSerial())
                .nonce(req.getNonce())
                .signature(req.getSignature())
                .timestamp(req.getTimestamp())
                .signType(Constants.SIGN_TYPE_RSA)
                .body(req.getBody())
                .build();
    }
}
