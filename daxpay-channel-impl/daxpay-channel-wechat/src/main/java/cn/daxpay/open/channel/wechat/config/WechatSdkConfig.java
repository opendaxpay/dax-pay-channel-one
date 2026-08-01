package cn.daxpay.open.channel.wechat.config;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;

/// # 微信 SDK 客户端构建工具
///
/// 根据主应用下发的通道凭证 [WechatSdkCredential] 构建 WxJava [WxPayService], 支持两种验签模式:
///
/// ## 验签模式
/// - **支付公钥新模式**(优先): 凭证配置了 publicKeyId + publicKey 时启用, 免平台证书自动轮换(微信官方推荐)
/// - **平台证书模式**(兜底): publicKeyId 为空时, 由 SDK 自动下载并轮换平台证书
public class WechatSdkConfig {

    /// 根据通道凭证构建 [WxPayService]
    ///
    /// 必填字段: wxMchId / wxAppId / apiKeyV3 / privateKey / certSerialNo。
    /// 支付公钥模式 publicKeyId 非空时自动启用。
    public static WxPayService buildService(WechatSdkCredential credential) {
        // 必填校验(下单签名路径, appId 必填)
        if (StrUtil.hasBlank(credential.getWxMchId(), credential.getWxAppId(),
                credential.getApiKeyV3(), credential.getPrivateKey(), credential.getCertSerialNo())) {
            // 微信: 通道配置无效
            throw new ChannelServiceException(ChannelErrorCode.INVALID_CONFIG,
                    "channel.error.wechatInvalidConfig");
        }
        WxPayService service = new WxPayServiceImpl();
        service.setConfig(toConfig(credential));
        return service;
    }

    /// 回调验签专用构建: 不要求 appId
    ///
    /// 回调验签+解密仅需 apiKeyV3 与证书(平台证书模式还需 mchId 做证书自动下载鉴权),
    /// 不依赖 wxAppId; appId 为空时 WxJava 解析回调不受影响。
    public static WxPayService buildCallbackService(WechatSdkCredential credential) {
        // 回调必填: apiKeyV3 + 证书 + mchId(平台证书模式证书下载鉴权)
        if (StrUtil.hasBlank(credential.getWxMchId(), credential.getApiKeyV3(),
                credential.getPrivateKey(), credential.getCertSerialNo())) {
            // 微信: 通道配置无效
            throw new ChannelServiceException(ChannelErrorCode.INVALID_CONFIG,
                    "channel.error.wechatInvalidConfig");
        }
        WxPayService service = new WxPayServiceImpl();
        service.setConfig(toConfig(credential));
        return service;
    }

    /// 凭证 → WxPayConfig(appId 为空时仅影响下单签名, 不影响回调验签)
    private static WxPayConfig toConfig(WechatSdkCredential credential) {
        WxPayConfig config = new WxPayConfig();
        config.setAppId(credential.getWxAppId());
        config.setMchId(credential.getWxMchId());
        config.setApiV3Key(credential.getApiKeyV3());
        // 商户私钥(PEM PKCS#8 字符串)
        config.setPrivateKeyString(credential.getPrivateKey());
        // 商户证书序列号
        config.setCertSerialNo(credential.getCertSerialNo());

        // 支付公钥新模式(优先): 配置了 publicKeyId 则启用, 免平台证书轮换
        if (StrUtil.isNotBlank(credential.getPublicKeyId())) {
            config.setPublicKeyId(credential.getPublicKeyId());
            config.setPublicKeyString(credential.getPublicKey());
        }

        // 服务商模式: 将特约商户信息(sub_mchid/sub_appid)注入 WxPayConfig
        if (StrUtil.isNotBlank(credential.getSubMchId())) {
            config.setSubMchId(credential.getSubMchId());
            if (StrUtil.isNotBlank(credential.getSubAppId())) {
                config.setSubAppId(credential.getSubAppId());
            }
        }
        return config;
    }
}
