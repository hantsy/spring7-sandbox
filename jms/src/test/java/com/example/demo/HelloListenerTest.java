package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringJUnitConfig(value = {HelloListenerTest.TestConfig.class})
@ContextConfiguration(initializers = {ArtemisContainerInitializer.class})
public class HelloListenerTest {
    private final static Logger log = LoggerFactory.getLogger(HelloListenerTest.class);

    @Configuration
    @Import(value = {JmsConfig.class, HelloListener.class})
    static class TestConfig {
    }

    @Autowired
    JmsTemplate jmsTemplate;

    @Autowired
    HelloListener receiver;

    @Test
    public void testHelloListener() {
        jmsTemplate.convertAndSend("hello", "Hello");

        // wait one second to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> {
                    List<String> received = receiver.received;
                    log.debug(">>> received: {}", received);

                    assertThat(received.size()).isEqualTo(1);
                    assertThat(received.getFirst()).isEqualTo("Hello");
                });
    }
}
