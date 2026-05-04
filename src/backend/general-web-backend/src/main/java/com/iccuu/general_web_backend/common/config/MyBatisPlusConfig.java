package com.iccuu.general_web_backend.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.iccuu.general_web_backend.infrastructure.snowflake.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus configuration.
 *
 * WARNING: Do NOT add {@code activateDefaultTyping} to the Spring Boot ObjectMapper
 * (in JacksonConfig or elsewhere). {@code JacksonTypeHandler} uses the global ObjectMapper
 * to deserialize JSON columns — enabling polymorphic typing would allow arbitrary class
 * instantiation from database-stored JSON, a deserialization gadget attack vector.
 * This is especially critical for {@code AuditLog.detail} (typed {@code Object}).
 *
 * If a future change requires polymorphic typing (e.g., in RedisConfig), use a separate
 * ObjectMapper instance scoped to that serializer only — never the global one.
 */
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public IdentifierGenerator idGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        return entity -> snowflakeIdGenerator.nextId();
    }
}
