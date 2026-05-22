package com.iccuu.general_web_backend.steamcache;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.iccuu.general_web_backend")
@MapperScan("com.iccuu.general_web_backend")
@EnableScheduling
public class SteamCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(SteamCacheApplication.class, args);
    }
}
