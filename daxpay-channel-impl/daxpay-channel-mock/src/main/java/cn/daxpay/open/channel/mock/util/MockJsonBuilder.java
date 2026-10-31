package cn.daxpay.open.channel.mock.util;

import java.util.LinkedHashMap;
import java.util.Map;

/// # 网关 snake_case 报文构造器
///
/// 服务层 camelCase 内部 DTO → snake_case 网关报文的统一构造入口:
/// null 值与空白字符串字段自动跳过(不向网关发送空字段), 保持字段插入序(签名与发送同串)。
public final class MockJsonBuilder {

    private final Map<String, Object> body = new LinkedHashMap<>();

    private MockJsonBuilder() {
    }

    /// 创建构造器
    public static MockJsonBuilder create() {
        return new MockJsonBuilder();
    }

    /// 追加字段(null/空白字符串自动跳过)
    public MockJsonBuilder put(String key, Object value) {
        if (value == null || (value instanceof String str && str.isBlank())) {
            return this;
        }
        this.body.put(key, value);
        return this;
    }

    /// 构造报文 Map
    public Map<String, Object> build() {
        return this.body;
    }
}
