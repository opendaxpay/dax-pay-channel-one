package cn.daxpay.open.channel.mock.service.callback;

import cn.daxpay.open.channel.mock.req.MockCallbackParseReq;
import cn.daxpay.open.channel.mock.resp.MockCallbackParseResp;
import cn.daxpay.open.channel.mock.util.MockDateUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # Mock 通道退款回调解析服务
///
/// 不出站: 平台公钥验签后取 data 业务报文, snake→camel 转 [MockCallbackParseResp]
/// (与支付回调共用 resp 形态); 退款回调 data 字段按契约定稿:
/// 在支付字段之上补 out_refund_no/refund_id, amount 为退款金额。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockRefundCallbackParseService {

    private final MockCallbackSupport callbackSupport;

    /// 退款回调解析
    public MockCallbackParseResp parse(MockCallbackParseReq req) {
        JSONObject data = callbackSupport.verifyAndParse(
                req.getCredential(), req.getBody(), req.getSign(), req.getTimestamp(), req.getNonce());
        log.info("Mock 通道退款回调解析: outRefundNo={}, status={}",
                data.getStr("out_refund_no"), data.getStr("status"));
        MockCallbackParseResp resp = new MockCallbackParseResp();
        resp.setSuccess(true);
        resp.setTradeType(data.getStr("mock_trade_type"));
        resp.setOutTradeNo(data.getStr("out_trade_no"));
        resp.setTradeNo(data.getStr("trade_no"));
        resp.setOutRefundNo(data.getStr("out_refund_no"));
        resp.setRefundId(data.getStr("refund_id"));
        resp.setStatus(data.getStr("status"));
        resp.setAmount(data.getLong("amount"));
        resp.setFinishTime(MockDateUtil.fromRfc3339(data.getStr("finish_time")));
        // 商户透传信息拟真回传(报文未携带为 null; 退款回调报文不含 openid, 自然为空)
        resp.setBizOrderNo(data.getStr("biz_order_no"));
        resp.setAttach(data.getStr("attach"));
        resp.setSubMchId(data.getStr("sub_mchid"));
        resp.setOpenId(data.getStr("openid"));
        return resp;
    }
}
