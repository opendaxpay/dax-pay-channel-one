package cn.daxpay.open.platform.system;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/// # 通道服务系统模块装配入口
///
/// 对标主项目 `SystemApplication`, 作为 service-system 模块的自动装配类。
/// 通过 `@ComponentScan` 扫描本模块及子包的组件(如 [cn.daxpay.open.platform.system.handler.exception.ChannelExceptionHandler]),
/// 由 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册,
/// 被 channel-start 启动类加载, 不依赖启动类组件扫描。
@AutoConfiguration
@ComponentScan
public class SystemApplication {
}
