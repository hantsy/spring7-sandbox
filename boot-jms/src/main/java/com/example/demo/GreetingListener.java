package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GreetingListener {
    public String latestMessage;

    @JmsListener(destination = DemoApplication.DESTENATION_HELLO)
    public void onGreeting(Greeting greeting) {
        log.debug("received greeting: {}", greeting);
        this.latestMessage = greeting.message();
    }
}
