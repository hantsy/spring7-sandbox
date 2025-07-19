package com.example.demo;

import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {RetryExample.class, RetryConfig.class})
public class RetryExampleTest {
    private static final Logger log = LoggerFactory.getLogger(RetryExampleTest.class);

    @Autowired
    RetryTemplate retryTemplate;

    @Autowired
    RetryExample retryExample;

    @Test
    public void test() {
        try {
            retryTemplate.execute(new Retryable<>() {
                @Override
                public @Nullable Object execute() throws Throwable {
                    retryExample.test();
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("exception:", e);
            assertThat(e).cause().isInstanceOf(ExampleException.class);
        }

        Awaitility.await().atMost(10_000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(retryExample.count()).isEqualTo(6));
    }
}
