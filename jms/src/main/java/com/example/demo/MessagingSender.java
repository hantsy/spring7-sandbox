package com.example.demo;

import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.stream.IntStream;

@Component
public class MessagingSender {
    private final JmsMessagingTemplate template;

    public MessagingSender(JmsMessagingTemplate messagingTemplate) {
        this.template = messagingTemplate;
    }

    public void send() {
        IntStream.range(1, 11)
                .forEach(i ->
                        template.convertAndSend("messagingHello",
                                new Greeting("Hello #"+ i, Instant.now()),
                                Map.of("_type", Greeting.class.getName()))
                );

    }
}
