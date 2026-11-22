package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.client.annotation.AmqpListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class GreetingListener {
    private final static Logger log = LoggerFactory.getLogger(GreetingListener.class.getName());

    public List<String> messages = new ArrayList<>();

    @AmqpListener(addresses = "/queues/hello")
    public void onGreeting(Greeting greeting) {
        log.debug("received greeting: {} at {}", greeting, LocalDateTime.now());
        messages.add(greeting.message());
    }
}
