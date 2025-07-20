package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HelloObjectReceiver {
    private final Logger log = LoggerFactory.getLogger(JmsConfig.class);

    private List<Greeting> messageList = new ArrayList<>();

    @JmsListener(destination = "helloObject")
    public void onMessage(Greeting message) {
        log.debug("receiving message: {}", message);
        messageList.add(message);
    }

    public List<Greeting> getMessageList() {
        return messageList;
    }
}
