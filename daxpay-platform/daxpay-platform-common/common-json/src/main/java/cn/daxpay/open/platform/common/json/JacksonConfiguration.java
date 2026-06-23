package cn.daxpay.open.platform.common.json;

import cn.daxpay.open.platform.common.json.jdk.Java8TimeFormatModule;
import cn.daxpay.open.platform.common.json.jdk.JavaLongTypeModule;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;

import java.util.TimeZone;

/// # Jackson 序列化自动配置
///
/// 使用 Spring Boot 4 标准的 JsonMapperBuilderCustomizer 定制机制, 不手动创建 ObjectMapper。
/// 与主项目 common-json 模块保持一致的序列化行为:
/// - Long → String (防止前端精度丢失)
/// - Java 8 时间类型格式定制 (OffsetDateTime → ISO UTC, LocalDate, LocalTime)
/// - 全局时区 UTC (与 OffsetDateTime + timestamptz(6) 规范一致)
///
@AutoConfiguration
public class JacksonConfiguration {

    /// 定制 JsonMapper Builder, 配置全局序列化行为
    ///
    /// 通过 Spring Boot 标准的 JsonMapperBuilderCustomizer 机制, 让 auto-configuration 创建的 JsonMapper 自动应用这些配置。
    /// Jackson 3 默认 WRITE_DATES_AS_TIMESTAMPS=false, 且已有 Java8TimeFormatModule 覆盖格式, 无需额外配置。
    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                // 指定要序列化的域
                .changeDefaultVisibility(vc -> vc.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY))
                // 忽略未知属性
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 对象属性为空时可以序列化
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // Long 类型序列化为字符串, 防止前端精度丢失
                .addModule(new JavaLongTypeModule())
                // Java 8 时间类型格式定制 (OffsetDateTime/LocalDate/LocalTime)
                .addModule(new Java8TimeFormatModule());
    }

    /// 初始化时设置全局时区为 UTC
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
