package com.example.demo;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

@Component
public class HelloObjectSender {
    private final JmsTemplate jmsTemplate;

    public HelloObjectSender(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void send() {
        IntStream.range(1, 11)
                .forEach(i ->
                        jmsTemplate.convertAndSend("helloObject", new Greeting("Hello #"+ i, Instant.now()))
                );

    }
}
