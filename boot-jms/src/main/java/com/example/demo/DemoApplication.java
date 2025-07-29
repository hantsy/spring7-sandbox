package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsClient;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}


@Component
class Sender {
    private final static Logger log = LoggerFactory.getLogger(Receiver.class);
    final JmsClient jmsClient;

    public Sender(JmsClient jmsClient) {
        this.jmsClient = jmsClient;
    }

    public void sendMessage(String message) {
        log.debug("sending message:{}", message);
        this.jmsClient.destination("hello")
                .send(message);
    }
}

@Component
class Receiver {
    private final static Logger log = LoggerFactory.getLogger(Receiver.class);

    private String lastReceivedMessage;

    @JmsListener(destination = "hello")
    public void receiveMessage(String message) {
        log.debug("received message: {0} " + message);
        this.lastReceivedMessage = message;
    }

    public String latestMessage() {
        return lastReceivedMessage;
    }
}