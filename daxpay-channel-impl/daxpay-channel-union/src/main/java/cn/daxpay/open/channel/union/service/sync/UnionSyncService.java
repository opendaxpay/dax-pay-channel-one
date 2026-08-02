package cn.daxpay.open.channel.union.service.sync;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.req.UnionSyncReq;
import cn.daxpay.open.channel.union.resp.UnionSyncResp;
import cn.daxpay.open.channel.union.sdk.UnionClient;
import cn.daxpay.open.channel.union.util.UnionDateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/// # 云闪付通道支付同步服务
///
/// 通过银联 queryTrans.do 接口查询订单最终状态。
///
/// 银联查询返回两层状态码:
/// - respCode: 查询请求是否成功(00=成功)
/// - origRespCode: 原交易结果(00=支付成功 / 03=处理中 / 05=已关闭)
///
/// 子应用统一映射为平台标准 tradeStatus(SUCCESS / PROGRESS / CLOSED)。
@Service
@Slf4j
@RequiredArgsConstructor
public class UnionSyncService {

    private final RestClient restClient;

    /// 同步云闪付支付订单状态
    public UnionSyncResp sync(UnionSyncReq req) {
        UnionClient client = new UnionClient(req.getCredential(), restClient);
        Map<String, Object> param = this.buildCommonParam(req.getCredential());
        param.put("orderId", req.getOutTradeNo());
        // 银联查询交易类型
        param.put("txnType", "00");
        param.put("txnSubType", "00");
        param.put("bizType", "000000");
        Map<String, String> response = client.queryTrans(param);
        return this.parseResp(req.getOutTradeNo(), response);
    }

    /// 解析查询响应
    private UnionSyncResp parseResp(String outTradeNo, Map<String, String> response) {
        UnionSyncResp resp = new UnionSyncResp().setOutTradeNo(outTradeNo);
        String respCode = response.get("respCode");
        // 查询本身失败, 订单状态未知, 视为进行中
        if (!"00".equals(respCode)) {
            resp.setTradeStatus("PROGRESS");
            resp.setErrorMsg(response.get("respMsg"));
            return resp;
        }
        // 原交易结果
        String origRespCode = response.get("origRespCode");
        resp.setTradeStatus(this.mapPayStatus(origRespCode));
        resp.setTotalAmount(parseLong(response.get("txnAmt")));
        resp.setPayTime(response.get("txnTime"));
        resp.setQueryId(response.get("queryId"));
        resp.setBuyerId(response.get("accNo"));
        return resp;
    }

    /// 支付状态映射: origRespCode 00→SUCCESS, 05→CLOSED, 其他→PROGRESS
    private String mapPayStatus(String origRespCode) {
        if (StrUtil.isBlank(origRespCode)) {
            return "PROGRESS";
        }
        return switch (origRespCode) {
            case "00" -> "SUCCESS";
            case "05" -> "CLOSED";
            default -> "PROGRESS";
        };
    }

    /// 字符串金额转 Long(单位: 分), 空值返回 null
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
