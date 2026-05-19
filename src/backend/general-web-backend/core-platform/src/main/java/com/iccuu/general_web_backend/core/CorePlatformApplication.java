package com.iccuu.general_web_backend.core;

import com.iccuu.general_web_backend.infrastructure.geetest.GeeTestProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.iccuu.general_web_backend")
@MapperScan("com.iccuu.general_web_backend")
@EnableScheduling
@EnableConfigurationProperties(GeeTestProperties.class)
public class CorePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(CorePlatformApplication.class, args);
    }
}
