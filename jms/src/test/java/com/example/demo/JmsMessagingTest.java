package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringJUnitConfig(value = {JmsMessagingTest.TestConfig.class})
@ContextConfiguration(initializers = {ArtemisContainerInitializer.class})
public class JmsMessagingTest {
    private final static Logger log = LoggerFactory.getLogger(JmsMessagingTest.class);

    @Configuration
    @Import(value = {JmsConfig.class, MessagingSender.class, MessagingReceiver.class})
    static class TestConfig {
    }

    @Autowired
    MessagingSender sender;

    @Autowired
    MessagingReceiver receiver;

    @Test
    public void whenWaitOneSecond_thenReceiverShouldReceiveAllMessages() {
        sender.send();

        // wait one second to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> assertThat(receiver.getMessageList().size()).isEqualTo(10));
    }
}
