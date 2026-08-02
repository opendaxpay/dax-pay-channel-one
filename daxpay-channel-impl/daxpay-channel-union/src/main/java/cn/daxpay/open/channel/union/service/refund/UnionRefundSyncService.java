package cn.daxpay.open.channel.union.service.refund;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.req.UnionRefundSyncReq;
import cn.daxpay.open.channel.union.resp.UnionRefundSyncResp;
import cn.daxpay.open.channel.union.sdk.UnionClient;
import cn.daxpay.open.channel.union.util.UnionDateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/// # 云闪付通道退款同步服务
///
/// 通过银联 queryTrans.do 接口查询退款单最终状态(orderId=退款单号)。
///
/// 统一状态码: SUCCESS(退款成功) / PROGRESS(处理中) / CLOSED(关闭)。
@Service
@Slf4j
@RequiredArgsConstructor
public class UnionRefundSyncService {

    private final RestClient restClient;

    /// 同步云闪付退款状态
    public UnionRefundSyncResp sync(UnionRefundSyncReq req) {
        UnionClient client = new UnionClient(req.getCredential(), restClient);
        Map<String, Object> param = this.buildCommonParam(req.getCredential());
        param.put("orderId", req.getOutRefundNo());
        param.put("txnType", "00");
        param.put("txnSubType", "00");
        param.put("bizType", "000000");
        Map<String, String> response = client.queryTrans(param);
        return this.parseResp(req.getOutRefundNo(), response);
    }

    /// 解析退款查询响应
    private UnionRefundSyncResp parseResp(String outRefundNo, Map<String, String> response) {
        UnionRefundSyncResp resp = new UnionRefundSyncResp().setOutRefundNo(outRefundNo);
        String respCode = response.get("respCode");
        if (!"00".equals(respCode)) {
            resp.setRefundStatus("PROGRESS");
            resp.setErrorMsg(response.get("respMsg"));
            return resp;
        }
        String origRespCode = response.get("origRespCode");
        resp.setRefundStatus(this.mapRefundStatus(origRespCode));
        resp.setRefundAmount(parseLong(response.get("txnAmt")));
        resp.setFinishTime(response.get("txnTime"));
        return resp;
    }

    /// 退款状态映射: origRespCode 00→SUCCESS, 05→CLOSED, 其他→PROGRESS
    private String mapRefundStatus(String origRespCode) {
        if (StrUtil.isBlank(origRespCode)) {
            return "PROGRESS";
        }
        return switch (origRespCode) {
            case "00" -> "SUCCESS";
            case "05" -> "CLOSED";
            default -> "PROGRESS";
        };
    }

    private Long parseLong(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
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
