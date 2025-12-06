package com.example.demo;

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.cassandra.autoconfigure.DriverConfigLoaderBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    DriverConfigLoaderBuilderCustomizer customizer() {
        return builder -> builder
                .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofMillis(15_000));
    }

}
