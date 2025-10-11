package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.hash.Jackson3HashMapper;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@SpringBootApplication
@Slf4j
public class DemoApplication {
    public static final String STEAM_NAME = "demo";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    HashMapper hashMapper() {
        return new Jackson3HashMapper(new JsonMapper(), true);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    StreamMessageListenerContainer streamMessageListenerContainer(RedisConnectionFactory connectionFactory, HashMapper hashMapper) {
        StreamMessageListenerContainerOptions<String, ObjectRecord<String, Object>> options = StreamMessageListenerContainerOptions.builder()
                .objectMapper(hashMapper)
                .pollTimeout(Duration.ofMillis(5_000))
                .targetType(Greeting.class)
                .errorHandler(t -> log.debug("caught exception: {}", t.getMessage()))
                .build();

        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

}
