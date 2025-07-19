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

@SpringJUnitConfig(value = {JmsTest.TestConfig.class})
@ContextConfiguration(initializers = {ArtemisContainerInitializer.class})
public class JmsTest {
    private final static Logger log = LoggerFactory.getLogger(JmsTest.class);

    @Configuration
    @ComponentScan(basePackageClasses = Sender.class)
    @Import(value = {JmsConfig.class, Sender.class, Receiver.class})
    static class TestConfig {
    }

    @Autowired
    Sender sender;

    @Autowired
    Receiver receiver;

    @Test
    public void whenWaitOneSecond_thenReceiverShouldReceiveAllMessages() {
        sender.send();

        // wait one second to verify.
        await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> assertThat(receiver.getMessageList().size()).isEqualTo(10));
    }
}
