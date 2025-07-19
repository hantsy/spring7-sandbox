package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
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
    @ComponentScan(basePackageClasses = Sender.class)
    @Import(value = {JmsConfig.class})
    static class TestConfig {
    }

    @Autowired
    JmsMessagingTemplate template;

    @Test
    public void sendAndReceiveMessagesViaJmsMessagingTemplate() {
        template.convertAndSend("test", new Greeting("Hello JmsClient!", Instant.now()));

        // wait one second to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> {
                    var receivedMessage = template.receiveAndConvert("test", Greeting.class);
                    assertThat(receivedMessage).isNotNull();
                    log.info("Greeting messages received via JmsMessagingTemplate: {}", receivedMessage);
                    assertThat(receivedMessage.message()).isEqualTo("Hello JmsClient!");
                });
    }
}
