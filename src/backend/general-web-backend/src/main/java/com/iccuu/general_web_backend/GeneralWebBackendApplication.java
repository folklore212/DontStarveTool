package com.iccuu.general_web_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan({"com.iccuu.general_web_backend.module.**.mapper",
             "com.iccuu.general_web_backend.infrastructure.storage"})
@ConfigurationPropertiesScan("com.iccuu.general_web_backend")
@EnableScheduling
public class GeneralWebBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeneralWebBackendApplication.class, args);
    }

}
