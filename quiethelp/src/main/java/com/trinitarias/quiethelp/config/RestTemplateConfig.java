package com.trinitarias.quiethelp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();  // Crea una instancia para inyectar donde sea -peticiones http-
    }
}