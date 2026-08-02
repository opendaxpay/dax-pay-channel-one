package cn.daxpay.open.channel.union.service.close;

import cn.daxpay.open.channel.union.config.UnionSdkCredential;
import cn.daxpay.open.channel.union.req.UnionCloseReq;
import cn.daxpay.open.channel.union.resp.UnionCloseResp;
import cn.daxpay.open.channel.union.sdk.UnionClient;
import cn.daxpay.open.channel.union.util.UnionDateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/// # 云闪付通道关单服务
///
/// 银联 ACP 关单(交易类型 31)需要原交易查询凭证 queryId(origQryId)。
@Service
@Slf4j
@RequiredArgsConstructor
public class UnionCloseService {

    private final RestClient restClient;

    /// 关闭云闪付支付订单
    public UnionCloseResp close(UnionCloseReq req) {
        UnionClient client = new UnionClient(req.getCredential(), restClient);
        Map<String, Object> param = this.buildCommonParam(req.getCredential());
        param.put("orderId", req.getOutTradeNo());
        param.put("origQryId", req.getQueryId());
        client.closeOrder(param);
        log.info("云闪付关单成功: outTradeNo={}", req.getOutTradeNo());
        return new UnionCloseResp().setOutTradeNo(req.getOutTradeNo());
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
