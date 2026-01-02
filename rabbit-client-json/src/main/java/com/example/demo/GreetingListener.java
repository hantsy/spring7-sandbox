package com.example.demo;

import com.rabbitmq.client.amqp.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Component
class GreetingListener {
    private static final Logger log = LoggerFactory.getLogger(GreetingListener.class);
    final List<Greeting> received = new ArrayList<>();

    @Autowired
    JsonMapper jsonMapper;

    @RabbitListener(queues = RabbitClientConfig.HELLO_QUEUE_NAME,
            concurrency = "2",
            id = "helloListener",
            // there is no global message converter property in the listener container factory
            // see: https://github.com/spring-projects/spring-amqp/issues/3274
            messageConverter = "jsonMessageConverter"
    )
    void handleGreeting(/*Message data*/ Greeting greeting) {
//        log.info("Received data from RabbitMQ: {}", data);
//        Greeting greeting = jsonMapper.readValue(data.body(), Greeting.class);
        log.info("Converted message payload: {}", greeting);
        received.add(greeting);
    }
}
