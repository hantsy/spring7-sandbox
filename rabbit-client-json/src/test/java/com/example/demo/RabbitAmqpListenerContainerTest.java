package com.example.demo;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;

import static com.example.demo.RabbitClientConfig.HELLO_EXCHANGE_NAME;
import static com.example.demo.RabbitClientConfig.HELLO_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {
        RabbitAmqpListenerContainerTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class RabbitAmqpListenerContainerTest {
    private final Logger log = LoggerFactory.getLogger(RabbitAmqpListenerContainerTest.class);

    @Configuration
    @Import({JacksonJsonMapperConfig.class,
            RabbitClientConfig.class,
            GreetingListener.class
    })
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Autowired
    GreetingListener greetingListener;

    @Test
    void testGreetingListener() {
        log.debug("Start sending message...");
        rabbitAmqpTemplate.convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, new Greeting("Hello", Instant.now()))
                .whenComplete((aBoolean, throwable) -> log.debug("Sending message is completed!!!"));

        Awaitility.await().atMost(Duration.ofMillis(1_5000))
                .untilAsserted(() -> {
                    assertThat(greetingListener.received.size()).isEqualTo(1);
                });

    }

}
