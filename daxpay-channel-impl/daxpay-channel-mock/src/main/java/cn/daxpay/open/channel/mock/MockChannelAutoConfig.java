package cn.daxpay.open.channel.mock;

import cn.daxpay.open.channel.mock.config.MockGatewayProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/// # Mock 模拟通道组件装配
///
/// 本模块模拟"微信官方 SDK"角色, 经 [cn.daxpay.open.channel.mock.sdk.MockGatewayClient]
/// 以 V3 签名协议调用 10400 mock 网关(模拟"微信开放平台服务器")的 /v3/mock/** 端点。
///
/// DEV 沙箱开关: `daxpay.platform.deployment.mode` 非 DEV 时整个模块静默不装配,
/// PROD 下 one 不暴露 /channel/mock/** 端点。
///
/// 装配方式为 `@AutoConfiguration` + `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
/// 注册: 本包在启动类 [cn.daxpay.open.channel.ChannelServiceApp] 的扫描基包内, 若仅靠包扫描加载,
/// 条件只能挡住本配置类、挡不住同包 `@Component/@Service/@RestController`(PROD 下会因缺少
/// [MockGatewayProperties] 启动失败); 故启动类已把本包从包扫描中整体剔除, 由本类按条件决定整包装配。
@AutoConfiguration
@ConditionalOnProperty(name = "daxpay.platform.deployment.mode", havingValue = "DEV", matchIfMissing = true)
@ComponentScan(basePackages = "cn.daxpay.open.channel.mock")
@EnableConfigurationProperties(MockGatewayProperties.class)
public class MockChannelAutoConfig {
}
