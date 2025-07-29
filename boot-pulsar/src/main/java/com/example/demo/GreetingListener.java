package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class GreetingListener {
    public List<String> messages = new ArrayList<>();

    @PulsarListener(subscriptionName = "hello-subscription", topics = DemoApplication.TOPIC_HELLO)
    public void onGreeting(Greeting greeting) {
        log.debug("received greeting: {}", greeting);
        messages.add(greeting.message());
    }
}
