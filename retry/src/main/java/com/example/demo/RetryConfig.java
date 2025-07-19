package com.example.demo;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;

import java.time.Duration;

@Configuration
public class RetryConfig {
    private final Logger log = LoggerFactory.getLogger(RetryListener.class);

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate(
                RetryPolicy.builder()
                        .maxAttempts(5)
                        .delay(Duration.ofMillis(1500))
                        .jitter(Duration.ofMillis(50))
                        .includes(ExampleException.class)
                        .build()
        );
        retryTemplate.setRetryListener(new RetryListener() {
            @Override
            public void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable) {
                log.debug("beforeRetry: retryPolicy={}, retryable={}", retryPolicy.getClass().getSimpleName(), retryable.getClass().getSimpleName());
            }

            @Override
            public void onRetrySuccess(RetryPolicy retryPolicy, Retryable<?> retryable, @Nullable Object result) {
                log.debug("onRetrySuccess: retryPolicy={}, retryable={}, result={}", retryPolicy.getClass().getSimpleName(), retryable.getClass().getSimpleName(), result);
            }

            @Override
            public void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
                log.debug("onRetryFailure: retryPolicy={}, retryable={}, throwable={}", retryPolicy.getClass().getSimpleName(), retryable.getClass().getSimpleName(), throwable.getMessage());
            }

            @Override
            public void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
                log.debug("onRetryPolicyExhaustion: retryPolicy={}, retryable={}, throwable={}", retryPolicy.getClass().getSimpleName(), retryable.getClass().getSimpleName(), throwable.getMessage());
            }
        });

        return retryTemplate;
    }
}
