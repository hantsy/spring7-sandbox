package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HelloListener {
    private final Logger log = LoggerFactory.getLogger(HelloListener.class);

    public List<String> received = new ArrayList<>();

    @JmsListener(destination = "hello")
    public void onMessage(String message) {
        log.debug("receiving body: {}", message);
        received.add(message);
    }
}
