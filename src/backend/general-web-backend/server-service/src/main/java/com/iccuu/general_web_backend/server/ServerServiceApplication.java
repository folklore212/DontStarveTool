package com.iccuu.general_web_backend.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.iccuu.general_web_backend")
public class ServerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerServiceApplication.class, args);
    }
}
