package com.example.demo;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {
        RabbitAmqpListenerContainerTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class RabbitAmqpListenerContainerTest {

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

        rabbitAmqpTemplate.convertAndSend("e1", "k1", new Greeting("Hello", Instant.now()));

        Awaitility.await().atMost(Duration.ofMillis(1_5000))
                .untilAsserted(() -> {
                    assertThat(greetingListener.getMessageList().size()).isEqualTo(1);
                });

    }

}
