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
        // 必填校验
        if (StrUtil.hasBlank(credential.getWxMchId(), credential.getWxAppId(),
                credential.getApiKeyV3(), credential.getPrivateKey(), credential.getCertSerialNo())) {
            // 微信: 通道配置无效
            throw new ChannelServiceException(ChannelErrorCode.INVALID_CONFIG,
                    "channel.error.wechatInvalidConfig");
        }

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
        // 注意: partner 路径切换由 isv service 显式调 createPartnerOrderV3 等方法决定, 非此配置自动完成
        if (StrUtil.isNotBlank(credential.getSubMchId())) {
            config.setSubMchId(credential.getSubMchId());
            // subAppId 可选(特约商户未配置自己的应用时留空, SDK 仅用 sp_appid + sub_mchid 走服务商模式)
            if (StrUtil.isNotBlank(credential.getSubAppId())) {
                config.setSubAppId(credential.getSubAppId());
            }
        }

        WxPayService service = new WxPayServiceImpl();
        service.setConfig(config);
        return service;
    }
}
