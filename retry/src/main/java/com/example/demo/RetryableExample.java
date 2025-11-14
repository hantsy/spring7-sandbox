package com.example.demo;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RetryableExample {

    private final Logger log = LoggerFactory.getLogger(RetryableExample.class);

    private int retryCount = 0;

    @Retryable(value = {ExampleException.class},
            maxRetries = 5,
            delay = 1500,
            jitter = 50,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public void test() {
        retryCount++;
        log.info("RetryableExample test:{}", retryCount);
        throw new ExampleException();
    }

    public int count() {
        return this.retryCount;
    }
}

