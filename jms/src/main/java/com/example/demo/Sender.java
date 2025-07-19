package com.example.demo;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

@Component
public class Sender {
    private final JmsTemplate jmsTemplate;

    public Sender(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void send() {
        IntStream.range(1, 11)
                .forEach(i ->
                        jmsTemplate.convertAndSend("hello", "Hello " + i + " at " + LocalDateTime.now())
                );

    }
}
