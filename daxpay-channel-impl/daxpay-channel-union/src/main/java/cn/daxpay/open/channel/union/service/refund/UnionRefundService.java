package cn.daxpay.open.channel.union.service.refund;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.req.UnionRefundReq;
import cn.daxpay.open.channel.union.resp.UnionRefundResp;
import cn.daxpay.open.channel.union.sdk.UnionClient;
import cn.daxpay.open.channel.union.util.UnionDateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/// # 云闪付通道退款服务
///
/// 银联 ACP 退货(交易类型 04)必须传入原交易查询凭证 origQueryId(支付成功时银联返回的 queryId)。
/// 退款状态: respCode=00(成功) / 03(处理中) / 其他(失败)。
@Service
@Slf4j
@RequiredArgsConstructor
public class UnionRefundService {

    private final RestClient restClient;

    /// 发起云闪付退款
    public UnionRefundResp refund(UnionRefundReq req) {
        if (StrUtil.isBlank(req.getOrigQueryId())) {
            throw new IllegalArgumentException("退款需传入原交易查询凭证 origQueryId");
        }
        UnionClient client = new UnionClient(req.getCredential(), restClient);
        Map<String, Object> param = this.buildCommonParam(req.getCredential());
        param.put("orderId", req.getOutRefundNo());
        param.put("origQryId", req.getOrigQueryId());
        param.put("txnAmt", req.getRefundAmount());
        param.put("backUrl", req.getNotifyUrl());
        Map<String, String> response = client.refund(param);
        String respCode = response.get("respCode");
        String finishTime = response.get("txnTime");
        String refundStatus = this.mapRefundStatus(respCode);
        log.info("云闪付退款: outRefundNo={}, respCode={}", req.getOutRefundNo(), respCode);
        return new UnionRefundResp()
                .setOutRefundNo(req.getOutRefundNo())
                .setRefundStatus(refundStatus)
                .setFinishTime(finishTime);
    }

    /// 退款状态映射: 00→SUCCESS, 03→PROCESSING, 其他→FAIL
    private String mapRefundStatus(String respCode) {
        if (StrUtil.isBlank(respCode)) {
            return "FAIL";
        }
        return switch (respCode) {
            case "00" -> "SUCCESS";
            case "03" -> "PROCESSING";
            default -> "FAIL";
        };
    }

    /// 构建银联公共参数
    private Map<String, Object> buildCommonParam(UnionSdkCredential cred) {
        Map<String, Object> param = new HashMap<>();
        param.put("version", "5.1.0");
        param.put("encoding", "UTF-8");
        param.put("merId", cred.getMerId());
        param.put("txnTime", UnionDateUtil.txnTime());
        param.put("accessType", "0");
        param.put("currencyCode", "156");
        return param;
    }
}
