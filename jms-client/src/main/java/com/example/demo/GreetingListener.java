package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GreetingListener {
    private final Logger log = LoggerFactory.getLogger(GreetingListener.class);

    public List<Greeting> received = new ArrayList<>();

    @JmsListener(destination = "greeting")
    public void onMessage(Greeting message) {
        log.debug("receiving body: {}", message);
        received.add(message);
    }
}
