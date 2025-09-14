package com.example.demo;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {RetryableExample.class, ResilientConfig.class})
public class RetryableExampleTest {

    @Autowired
    RetryableExample retryableExample;
    ;

    @Test
    public void test() {
        try {
            retryableExample.test();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ExampleException.class);
        }
        Awaitility.await().atMost(10_000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(retryableExample.count()).isEqualTo(6));
    }
}
