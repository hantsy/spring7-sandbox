package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;

@Component
public class ConcurrencyLimitExample {

    private final Logger log = LoggerFactory.getLogger(ConcurrencyLimitExample.class);

    private int count = 0;

    @ConcurrencyLimit(limit = 5)
    public void test(Integer i) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        count = i;
        log.info("RetryableExample test:{}, thread: {}", count, Thread.currentThread().getName());
    }

    public int count() {
        return this.count;
    }
}
