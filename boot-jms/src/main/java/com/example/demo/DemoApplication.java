package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;

@SpringBootApplication
public class DemoApplication {

    public static final String DESTINATION_HELLO = "hello";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    JacksonJsonMessageConverter jacksonMessageConverter() {
        JacksonJsonMessageConverter messageConverter = new JacksonJsonMessageConverter();
        messageConverter.setTypeIdPropertyName("_type");
        return messageConverter;
    }
}