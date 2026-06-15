# DaxPay Channel One — 通道适配子应用

通道适配子应用，用于对接单一第三方支付渠道，可作为新支付渠道接入的模板工程。

## 子模块

| 模块 | 说明 |
|------|------|
| `daxpay-channel-core` | 核心定义 — DTO、常量、枚举、接口 |
| `daxpay-channel-impl` | 业务实现 — 下单、退款、查询、回调处理 |
| `daxpay-channel-start` | 启动入口 |

## 技术栈

- Java 25
- Spring Boot 4.0.6
- Lombok

## 构建

```bash
mvnd clean install -DskipTests -T 4
```

## 接入说明

1. 复制本模块并重命名
2. 修改 `pom.xml` 中的 `artifactId` 与包名
3. 在 `core` 中定义渠道 DTO、枚举、常量
4. 在 `impl` 中实现支付/退款/查询/回调接口
5. 在 `daxpay-start` 中注册为 Spring Boot 子应用
