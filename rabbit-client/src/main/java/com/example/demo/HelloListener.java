package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.demo.RabbitClientConfig.HELLO_QUEUE_NAME;

@Component
class HelloListener {
    private static final Logger log = LoggerFactory.getLogger(HelloListener.class);
    final List<String> received = Collections.synchronizedList(new ArrayList<>());

    @RabbitListener(queues = {HELLO_QUEUE_NAME}, id = "testHelloListener")
    void processHello(String data) {
        log.debug(":: received data: [{}]", data);
        this.received.add(data);
    }
}
