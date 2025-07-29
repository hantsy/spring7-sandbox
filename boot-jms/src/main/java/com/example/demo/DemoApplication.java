package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static final String DESTENATION_HELLO = "hello";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    org.springframework.jms.support.converter.JacksonJsonMessageConverter jacksonMessageConverter() {
        org.springframework.jms.support.converter.JacksonJsonMessageConverter messageConverter = new org.springframework.jms.support.converter.JacksonJsonMessageConverter();
        messageConverter.setTypeIdPropertyName("_type");
        return messageConverter;
    }


//    @Bean
//    JacksonJsonMessageConverter jacksonJsonMessageConverter() {
//        return new JacksonJsonMessageConverter();
//    }

}