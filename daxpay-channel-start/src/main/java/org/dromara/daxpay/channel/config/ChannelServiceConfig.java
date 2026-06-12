package org.dromara.daxpay.channel.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Configuration
public class ChannelServiceConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        mapper.registerModule(longToStringModule());
        return mapper;
    }

    private SimpleModule longToStringModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, new StdSerializer<Long>(Long.class) {
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws java.io.IOException {
                gen.writeString(value.toString());
            }
        });
        module.addSerializer(Long.TYPE, new StdSerializer<Long>(Long.TYPE) {
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws java.io.IOException {
                gen.writeString(value.toString());
            }
        });
        return module;
    }
}
