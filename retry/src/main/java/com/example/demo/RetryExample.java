package com.example.demo;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RetryExample {

    private final Logger log = LoggerFactory.getLogger(RetryExample.class);

    private int retryCount = 0;

    public void test() {
        retryCount++;
        log.info("RetryableExample test:{}", retryCount);
        throw new ExampleException();
    }

    public int count() {
        return this.retryCount;
    }
}

