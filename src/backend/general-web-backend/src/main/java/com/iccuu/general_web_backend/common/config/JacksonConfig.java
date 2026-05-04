package com.iccuu.general_web_backend.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longIdSerializerCustomizer() {
        return builder -> builder.serializerByType(Long.class, new SafeLongSerializer());
    }

    /**
     * Serializes Long values as strings only when they exceed JavaScript's
     * safe integer range (2^53 - 1), preventing precision loss for Snowflake IDs
     * while keeping small longs (page/size/total/status flags) as numbers.
     */
    private static class SafeLongSerializer extends JsonSerializer<Long> {
        private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;
        private static final long JS_MIN_SAFE_INTEGER = -9007199254740991L;

        @Override
        public void serialize(Long value, JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            if (value > JS_MAX_SAFE_INTEGER || value < JS_MIN_SAFE_INTEGER) {
                gen.writeString(value.toString());
                return;
            }
            gen.writeNumber(value);
        }
    }
}
