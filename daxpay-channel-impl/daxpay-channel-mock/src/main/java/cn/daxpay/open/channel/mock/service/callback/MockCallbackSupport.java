package cn.daxpay.open.channel.mock.service.callback;

import cn.daxpay.open.channel.mock.config.MockSdkCredential;
import cn.daxpay.open.channel.mock.sdk.MockPemUtil;
import cn.daxpay.open.channel.mock.sdk.MockSignVerifier;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

/// # Mock 通道回调解析共用支撑
///
/// 回调报文为明文单层结构(外层 event_type/summary/out_biz_no + data 业务 JSON, 无加密壳),
/// 签名信息由主应用原样转发(sign/timestamp/nonce 三值, 对应 10400 回调签名三头)。
/// 验签链路: 平台公钥对三行验签串 ``{timestamp}\n{nonce}\n{body}\n`` 做 RSA-SHA256 校验
/// → 时间窗校验(±300 秒, 与 10400 网关过滤器 MockGatewaySignFilter 口径一致) → 取 data 业务 JSON 返回。
/// 任一环节失败均 fail-closed(抛回调验签失败业务异常, 错误信息区分缺失签名头/缺公钥/
/// 签名不匹配/时间窗超限); **不做 nonce 防重放**——回调按订单状态天然幂等(已终态单重复通知
/// 走幂等跳过), 无状态服务不做防重放存储。
/// 支付/退款两个 parse 端点共用, 均不出站(回调来自 10400 经主应用原样转发)。
@Component
public class MockCallbackSupport {

    /// 回调时间戳容忍窗口(秒), 对齐 10400 网关过滤器 MockGatewaySignFilter 的 ±300 秒口径
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;

    /// 验签回调 body 并取 data 业务 JSON(snake_case)
    public JSONObject verifyAndParse(MockSdkCredential credential, String body,
                                     String sign, String timestamp, String nonce) {
        // 平台公钥缺失单独判别(fail-closed: 无公钥无法验签, 给出可定位的中文错误)
        if (credential == null || StrUtil.isBlank(credential.getPlatformPublicKey())) {
            throw verifyFailed("mock.error.callbackPlatformKeyMissing");
        }
        // 时间窗校验(签名校验前先卡时间, 非法/超窗直接拒绝)
        checkTimestamp(timestamp);
        try {
            MockSignVerifier.verify(MockPemUtil.loadPublicKey(credential.getPlatformPublicKey()),
                    sign, timestamp, nonce, body);
        } catch (IllegalArgumentException e) {
            // 签名头缺失/签名不匹配等确定性失败, 透传具体原因
            throw verifyFailed("mock.error.callbackVerifyFailed", e.getMessage());
        } catch (Exception e) {
            // 公钥 PEM 解析失败等异常场景
            throw verifyFailed("mock.error.callbackPlatformKeyParseFailed", e.getMessage());
        }
        JSONObject root;
        try {
            root = JSONUtil.parseObj(body);
        } catch (Exception e) {
            throw verifyFailed("mock.error.callbackBodyParseFailed", e.getMessage());
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            throw verifyFailed("mock.error.callbackDataMissing");
        }
        return data;
    }

    /// 时间戳窗口校验: 非秒级数字或偏离当前时间超过 ±300 秒均拒绝
    private void checkTimestamp(String timestamp) {
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (Exception e) {
            throw verifyFailed("mock.error.callbackTimestampInvalid");
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - ts) > TIMESTAMP_TOLERANCE_SECONDS) {
            throw verifyFailed("mock.error.callbackTimestampOutOfWindow", TIMESTAMP_TOLERANCE_SECONDS);
        }
    }

    /// 构造回调验签失败业务异常(统一错误码, 文案取 i18n key, 支持 {0} 占位)
    private ChannelServiceException verifyFailed(String messageKey, Object... args) {
        return new ChannelServiceException(ChannelErrorCode.CALLBACK_VERIFY_FAILED.getCode(),
                messageKey, args);
    }
}
