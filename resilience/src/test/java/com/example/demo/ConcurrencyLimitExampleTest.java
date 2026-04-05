package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

@SpringJUnitConfig(classes = {
        ConcurrencyLimitExampleTest.AsyncConfig.class,
        ConcurrencyLimitExample.class,
        ResilienceConfig.class
})
public class ConcurrencyLimitExampleTest {

    @Autowired
    ConcurrencyLimitExample example;

    @Autowired
    Executor taskExecutor;

    @Configuration
    static class AsyncConfig {

        public AsyncConfig() {
        }

        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(3); // Number of concurrent threads
            executor.setMaxPoolSize(20);
            executor.setQueueCapacity(500);
            executor.setThreadNamePrefix("MyParallelThread-");
            executor.initialize();
            return executor;
        }
    }

    @Test
    public void test() {
        try {
            IntStream.range(0, 10)
                    .forEach(i -> {
                        CompletableFuture.runAsync(() -> example.test(i), taskExecutor);
                    });

        } catch (Exception e) {
        }
    }
}
