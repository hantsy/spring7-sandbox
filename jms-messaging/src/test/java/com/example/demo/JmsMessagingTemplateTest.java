package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringJUnitConfig(value = {JmsMessagingTemplateTest.TestConfig.class})
@ContextConfiguration(initializers = {ArtemisContainerInitializer.class})
public class JmsMessagingTemplateTest {
    private final static Logger log = LoggerFactory.getLogger(JmsMessagingTemplateTest.class);

    @Configuration
    @Import(value = {JmsConfig.class})
    static class TestConfig {
    }

    @Autowired
    JmsMessagingTemplate jmsMessagingTemplate;

    @Test
    public void testSendAndReceive() {
        jmsMessagingTemplate.convertAndSend("test", "hello");

        // wait to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> assertThat(jmsMessagingTemplate.receiveAndConvert("test", String.class)).isEqualTo("hello"));
    }

    @Test
    public void testSendAndReceive_GreetingObject() {
        jmsMessagingTemplate.convertAndSend("testObject", new Greeting("Hello JmsClient!", Instant.now()));

        // wait one second to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> {
                    var receivedMessage = jmsMessagingTemplate.receiveAndConvert("testObject", Greeting.class);
                    assertThat(receivedMessage).isNotNull();
                    log.info("Greeting messages received via JmsMessagingTemplate: {}", receivedMessage);
                    assertThat(receivedMessage.body()).isEqualTo("Hello JmsClient!");
                });
    }
}
