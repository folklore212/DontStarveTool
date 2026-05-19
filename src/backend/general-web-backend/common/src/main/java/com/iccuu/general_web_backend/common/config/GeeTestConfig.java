package com.iccuu.general_web_backend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Configuration
public class GeeTestConfig {

    private Login login = new Login();
    private Register register = new Register();

    @Data
    public static class Login {
        private String captchaId;
        private String captchaKey;
    }

    @Data
    public static class Register {
        private String captchaId;
        private String captchaKey;
    }

    public Login getLogin() {
        return login;
    }

    public void setLogin(Login login) {
        this.login = login;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    @Bean
    public RestTemplate geeTestRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        for (var converter : restTemplate.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
                var types = new ArrayList<>(jackson.getSupportedMediaTypes());
                types.add(MediaType.valueOf("text/javascript"));
                jackson.setSupportedMediaTypes(types);
            }
        }
        return restTemplate;
    }
}
