package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;

import static com.example.demo.RabbitClientConfig.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {
        RabbitAmqpTemplateTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class RabbitAmqpTemplateTest {

    @Configuration
    @Import({
            JacksonJsonMapperConfig.class,
            RabbitClientConfig.class
    })
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Test
    void testSendAndReceive_JSON() throws Exception {
        assertThat(this.rabbitAmqpTemplate.convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, new Greeting("test", Instant.now())))
                .succeedsWithin(Duration.ofSeconds(10));

        assertThat(this.rabbitAmqpTemplate.receiveAndConvert(HELLO_QUEUE_NAME, ParameterizedTypeReference.<Greeting>forType(Greeting.class)))
                .succeedsWithin(Duration.ofSeconds(10))
                .matches(it -> it.body().equals("test"));
    }
}
