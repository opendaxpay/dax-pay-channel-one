package cn.daxpay.open.channel.mock.service.callback;

import cn.daxpay.open.channel.mock.req.MockCallbackParseReq;
import cn.daxpay.open.channel.mock.resp.MockCallbackParseResp;
import cn.daxpay.open.channel.mock.util.MockDateUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # Mock 通道支付回调解析服务
///
/// 不出站: 平台公钥验签后取 data 业务报文, snake→camel 转 [MockCallbackParseResp]
/// (主应用回写逻辑复用, 零改动); 支付回调 data 字段按契约定稿:
/// mock_trade_type/out_trade_no/trade_no/status/amount/finish_time 等, 退款专属字段留空。
/// 商户透传信息(biz_order_no/attach/sub_mchid/openid)原样回传, 供主应用落回调记录与回填买家标识。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockPayCallbackParseService {

    /// 旧报文聚合渠道占位值(旧版回调以 provider 字段兼传实际付款钱包, 新报文 provider=订单渠道,
    /// 聚合单固定 AGGREGATE, 实付钱包改走 actual_provider)
    private static final String LEGACY_AGGREGATE_PROVIDER = "AGGREGATE";

    private final MockCallbackSupport callbackSupport;

    /// 支付回调解析
    public MockCallbackParseResp parse(MockCallbackParseReq req) {
        JSONObject data = callbackSupport.verifyAndParse(
                req.getCredential(), req.getBody(), req.getSign(), req.getTimestamp(), req.getNonce());
        log.info("Mock 通道支付回调解析: outTradeNo={}, status={}",
                data.getStr("out_trade_no"), data.getStr("status"));
        MockCallbackParseResp resp = new MockCallbackParseResp();
        resp.setSuccess(true);
        resp.setTradeType(data.getStr("mock_trade_type"));
        resp.setOutTradeNo(data.getStr("out_trade_no"));
        resp.setTradeNo(data.getStr("trade_no"));
        resp.setStatus(data.getStr("status"));
        resp.setAmount(data.getLong("amount"));
        resp.setFinishTime(MockDateUtil.fromRfc3339(data.getStr("finish_time")));
        // 商户透传信息拟真回传(报文未携带为 null)
        resp.setBizOrderNo(data.getStr("biz_order_no"));
        resp.setAttach(data.getStr("attach"));
        resp.setSubMchId(data.getStr("sub_mchid"));
        resp.setOpenId(data.getStr("openid"));
        // 聚合一码多付: 实际付款钱包(actual_provider 优先; 旧报文兼容——
        // 旧版聚合单以 provider 兼传实付钱包, 新报文 provider=AGGREGATE 非实付钱包置空)
        resp.setProvider(resolveActualProvider(data));
        return resp;
    }

    /// 解析实际付款钱包: 新报文取 actual_provider, 旧报文回落 provider(剔除聚合占位值)
    private String resolveActualProvider(JSONObject data) {
        String actualProvider = data.getStr("actual_provider");
        if (actualProvider != null) {
            return actualProvider;
        }
        String provider = data.getStr("provider");
        return LEGACY_AGGREGATE_PROVIDER.equals(provider) ? null : provider;
    }
}
