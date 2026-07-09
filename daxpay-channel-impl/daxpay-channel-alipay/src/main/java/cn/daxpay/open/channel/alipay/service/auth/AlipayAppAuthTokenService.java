package cn.daxpay.open.channel.alipay.service.auth;

import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.alipay.req.AlipayAppAuthTokenReq;
import cn.daxpay.open.channel.alipay.resp.AlipayAppAuthTokenResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayOpenAuthTokenAppModel;
import com.alipay.api.request.AlipayOpenAuthTokenAppRequest;
import com.alipay.api.response.AlipayOpenAuthTokenAppResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 支付宝应用授权令牌服务
///
/// 调用 `alipay.open.auth.token.app` 用授权码换取 app_auth_token, 供服务商代运营授权使用。
@Slf4j
@Service
public class AlipayAppAuthTokenService {

    /// 授权码换取应用授权令牌
    public AlipayAppAuthTokenResp exchange(AlipayAppAuthTokenReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        var request = new AlipayOpenAuthTokenAppRequest();
        var model = new AlipayOpenAuthTokenAppModel();
        // 授权码换 token
        model.setGrantType("authorization_code");
        model.setCode(req.getAuthCode());
        request.setBizModel(model);
        try {
            AlipayOpenAuthTokenAppResponse response = AlipaySdkConfig.execute(client, req.getCredential(), request);
            AlipayAppAuthTokenResp resp = new AlipayAppAuthTokenResp();
            resp.setCode(response.getCode());
            resp.setSubCode(response.getSubCode());
            resp.setSubMsg(response.getSubMsg());
            resp.setAppAuthToken(response.getAppAuthToken());
            resp.setAppRefreshToken(response.getAppRefreshToken());
            resp.setAuthAppId(response.getAuthAppId());
            resp.setUserId(response.getUserId());
            resp.setExpiresIn(response.getExpiresIn());
            resp.setReExpiresIn(response.getReExpiresIn());
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝换取应用授权令牌失败: err={}", e.getErrMsg());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayAppAuthTokenFailed", e.getErrMsg());
        }
    }
}
