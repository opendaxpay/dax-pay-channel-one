package cn.daxpay.open.channel.douyin.config;

import cn.daxpay.open.platform.core.exception.SdkCallException;
import com.douyinpay.api.DefaultDouyinpayClient;
import com.douyinpay.api.DouyinpayClient;
import com.douyinpay.api.notification.NotificationParser;
import com.douyinpay.api.payments.app.ApiAppPaymentsService;
import com.douyinpay.api.payments.h5.ApiH5PaymentsService;
import com.douyinpay.api.payments.jsapi.ApiJsapiPaymentsService;
import com.douyinpay.api.payments.nativepay.ApiNativePaymentsService;
import com.douyinpay.api.refund.ApiRefundService;
import com.douyinpay.define.AutoPlatformCertificateConfig;
import com.douyinpay.define.DomainName;
import lombok.experimental.UtilityClass;

import java.util.concurrent.ConcurrentHashMap;

/// # 抖音支付 SDK 客户端构建与调用工具
///
/// 根据主应用下发的通道凭证 [DouyinSdkCredential] 构建 [DouyinpayClient] 及各类业务 Service。
/// 凭证随请求下发(子应用无状态), 每次调用按凭证动态构建。
///
/// ## 回调验签
/// 平台证书自动拉取([AutoPlatformCertificateConfig])开销较大, 首次回调会请求抖音证书接口,
/// 拉取结果按 mchId 缓存([ConcurrentHashMap]), 避免重复拉取。
@UtilityClass
public class DouyinSdkConfig {

    /// 平台证书解析器缓存: key = mchId, 避免每次回调都重新拉取平台证书
    private static final ConcurrentHashMap<String, NotificationParser> NOTIFICATION_PARSER_CACHE = new ConcurrentHashMap<>();

    /// 构建抖音支付 SDK 客户端
    public DouyinpayClient buildClient(DouyinSdkCredential credential) {
        return new DefaultDouyinpayClient.AutoRSABuilder()
                .mchId(credential.getMchId())
                .merchantSerialNumber(credential.getMerchantSerialNumber())
                .privateKey(credential.getMerchantPrivateKey())
                .encryptKey(credential.getEncryptKey())
                .build();
    }

    /// 构建 NATIVE 扫码支付服务(关单/查单也复用此 Service, 抖音接口不区分支付方式)
    public ApiNativePaymentsService nativeService(DouyinSdkCredential credential) {
        return new ApiNativePaymentsService.Builder()
                .douyinpayClient(buildClient(credential))
                .domainName(DomainName.API)
                .build();
    }

    /// 构建 JSAPI 支付服务
    public ApiJsapiPaymentsService jsapiService(DouyinSdkCredential credential) {
        return new ApiJsapiPaymentsService.Builder()
                .douyinpayClient(buildClient(credential))
                .domainName(DomainName.API)
                .build();
    }

    /// 构建 APP 支付服务
    public ApiAppPaymentsService appService(DouyinSdkCredential credential) {
        return new ApiAppPaymentsService.Builder()
                .douyinpayClient(buildClient(credential))
                .domainName(DomainName.API)
                .build();
    }

    /// 构建 H5 支付服务
    public ApiH5PaymentsService h5Service(DouyinSdkCredential credential) {
        return new ApiH5PaymentsService.Builder()
                .douyinpayClient(buildClient(credential))
                .domainName(DomainName.API)
                .build();
    }

    /// 构建退款服务
    public ApiRefundService refundService(DouyinSdkCredential credential) {
        return new ApiRefundService.Builder()
                .douyinpayClient(buildClient(credential))
                .domainName(DomainName.API)
                .build();
    }

    /// 构建回调通知解析器(自动拉取平台证书验签)
    ///
    /// 按 mchId 缓存, 配置变更后需调用 [#invalidateNotificationParserCache] 清除
    public NotificationParser buildNotificationParser(DouyinSdkCredential credential) {
        return NOTIFICATION_PARSER_CACHE.computeIfAbsent(credential.getMchId(), mchId -> createNotificationParser(credential));
    }

    /// 清除回调解析器缓存(配置变更后调用)
    public void invalidateNotificationParserCache() {
        NOTIFICATION_PARSER_CACHE.clear();
    }

    private static NotificationParser createNotificationParser(DouyinSdkCredential credential) {
        AutoPlatformCertificateConfig certConfig = new AutoPlatformCertificateConfig.AutoRSAConfigBuilder()
                .mchId(credential.getMchId())
                .merchantSerialNumber(credential.getMerchantSerialNumber())
                .privateKey(credential.getMerchantPrivateKey())
                .encryptKey(credential.getEncryptKey())
                .build();
        return new NotificationParser(certConfig);
    }

    /// 构建客户端失败时抛出 SDK 调用异常(保留原始凭证标识便于排查)
    public static void assertClientNotNull(DouyinpayClient client, String mchId) {
        if (client == null) {
            throw new SdkCallException("抖音 SDK 客户端构建失败: mchId=" + mchId);
        }
    }
}
