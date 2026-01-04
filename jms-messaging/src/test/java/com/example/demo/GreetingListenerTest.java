package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringJUnitConfig(value = {GreetingListenerTest.TestConfig.class})
@ContextConfiguration(initializers = {ArtemisContainerInitializer.class})
public class GreetingListenerTest {
    private final static Logger log = LoggerFactory.getLogger(GreetingListenerTest.class);

    @Configuration
    @Import(value = {JmsConfig.class, GreetingListener.class})
    static class TestConfig {
    }

    @Autowired
    JmsMessagingTemplate jmsTemplate;

    @Autowired
    GreetingListener receiver;

    @Test
    public void testGreetingListener() {
        jmsTemplate.convertAndSend(
                "greeting",
                new Greeting("Hello", Instant.now()),
                message -> MessageBuilder.fromMessage(message)
                        .setHeader("_type", Greeting.class.getTypeName())
                        .build()
        );

        // wait to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> {
                    List<Greeting> received = receiver.received;
                    log.debug(">>> received: {}", received);

                    assertThat(received.size()).isEqualTo(1);
                    assertThat(received.getFirst().body()).isEqualTo("Hello");
                });
    }
}
