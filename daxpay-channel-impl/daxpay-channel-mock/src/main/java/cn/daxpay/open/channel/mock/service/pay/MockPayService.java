package cn.daxpay.open.channel.mock.service.pay;

import cn.daxpay.open.channel.mock.enums.MockPayBodyType;
import cn.daxpay.open.channel.mock.req.MockPayReq;
import cn.daxpay.open.channel.mock.resp.MockPayResp;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import cn.daxpay.open.channel.mock.util.MockDateUtil;
import cn.daxpay.open.channel.mock.util.MockJsonBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # Mock 通道支付下单服务
///
/// 内部 camelCase DTO ↔ 网关 snake_case 报文互转(字段映射按契约 §七命名对照):
/// mchNo→mchid、channelMchNo→sub_mchid、authCode→auth_code、appId→app_id、bizOrderNo→biz_order_no、
/// goodsDetail→goods_detail、expireTime(yyyy-MM-dd HH:mm:ss)→RFC3339 expire_time、notifyUrl→notify_url; 响应逐字段 snake→camel。
@Service
@Slf4j
@RequiredArgsConstructor
public class MockPayService {

    private final MockGatewayClient gatewayClient;

    /// 支付下单(POST /v3/mock/pay, 挂起族返 pay_body)
    public MockPayResp pay(MockPayReq req) {
        log.info("Mock 通道支付下单: outTradeNo={}, amount={}, way={}", req.getOutTradeNo(), req.getAmount(), req.getWay());
        String mchid = MockGatewayClient.resolveMchid(req.getMchNo(), req.getChannelMchNo());
        MockJsonBuilder builder = MockJsonBuilder.create()
                .put("mchid", mchid)
                .put("out_trade_no", req.getOutTradeNo())
                .put("amount", req.getAmount())
                .put("method", req.getMethod())
                .put("provider", req.getProvider())
                .put("way", req.getWay())
                .put("auth_code", req.getAuthCode())
                .put("subject", req.getSubject())
                .put("biz_order_no", req.getBizOrderNo())
                .put("description", req.getDescription())
                .put("goods_detail", req.getGoodsDetail())
                .put("attach", req.getAttach())
                .put("app_id", req.getAppId())
                // 买家 openid(微信 JSAPI/小程序校验链必填; 可空, 空不写入报文; 网关键 open_id)
                .put("open_id", req.getOpenId())
                .put("notify_url", req.getNotifyUrl());
        // 过期时间边界转换: 内部 yyyy-MM-dd HH:mm:ss → 网关 RFC3339(+08:00)
        if (StrUtil.isNotBlank(req.getExpireTime())) {
            builder.put("expire_time", MockDateUtil.internalToRfc3339(req.getExpireTime()));
        }
        // 双商户标识齐备时补 sub_mchid(服务商语境, 契约 §七样例)
        if (StrUtil.isNotBlank(req.getMchNo()) && StrUtil.isNotBlank(req.getChannelMchNo())) {
            builder.put("sub_mchid", req.getChannelMchNo());
        }
        JSONObject response = gatewayClient.post(req.getCredential(), mchid, "/v3/mock/pay", builder.build());
        MockPayResp resp = new MockPayResp();
        resp.setOutTradeNo(response.getStr("out_trade_no"));
        resp.setTradeNo(response.getStr("trade_no"));
        resp.setPayBody(response.getStr("pay_body"));
        resp.setPayBodyType(parsePayBodyType(response.getStr("pay_body_type")));
        resp.setComplete(response.getBool("complete"));
        resp.setFinishTime(MockDateUtil.fromRfc3339(response.getStr("finish_time")));
        resp.setAmount(response.getLong("amount"));
        return resp;
    }

    /// pay_body_type 串 → 枚举(与内部枚举名一致: URL/PARAMS/JSON, 契约 §七)
    private static MockPayBodyType parsePayBodyType(String value) {
        return value == null ? null : MockPayBodyType.valueOf(value);
    }
}
