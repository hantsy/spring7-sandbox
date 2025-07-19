package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringJUnitConfig(value = {JmsClientTest.TestConfig.class})
@ContextConfiguration(initializers = {ArtemisContainerInitializer.class})
public class JmsClientTest {
    private final static Logger log = LoggerFactory.getLogger(JmsClientTest.class);

    @Configuration
    @ComponentScan(basePackageClasses = Sender.class)
    @Import(value = {JmsConfig.class})
    static class TestConfig {
    }

    @Autowired
    JmsClient jmsClient;

    @Test
    public void sendAndReceiveMessagesViaJmsClient() {
        jmsClient.destination("test")
                .withTimeToLive(1000)
                .send(new Greeting("Hello JmsClient!", Instant.now()));

        // wait one second to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> {
                    var receivedMessage = jmsClient.destination("test")
                            .withTimeToLive(1000)
                            .receive(Greeting.class);
                    assertThat(receivedMessage).isPresent();
                    log.info("Greeting messages received: {}", receivedMessage.get());
                    assertThat(receivedMessage.get().message()).isEqualTo("Hello JmsClient!");
                });
    }
}
