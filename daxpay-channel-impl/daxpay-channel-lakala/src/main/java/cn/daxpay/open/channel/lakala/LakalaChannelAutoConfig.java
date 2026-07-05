package cn.daxpay.open.channel.lakala;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/// # 拉卡拉通道组件装配
///
/// 本子应用为单体部署, 由启动类 ChannelServiceApp 的 @SpringBootApplication 组件扫描加载,
/// 因此使用 @Configuration 而非 @AutoConfiguration(后者需 META-INF/spring imports 注册, 适用于作为 jar 被外部应用装配)
@Configuration
@ComponentScan(basePackages = "cn.daxpay.open.channel.lakala")
public class LakalaChannelAutoConfig {
}
