package com.iccuu.general_web_backend.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.iccuu.general_web_backend")
@EnableScheduling
public class CorePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(CorePlatformApplication.class, args);
    }
}
