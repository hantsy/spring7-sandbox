package com.example.demo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringJUnitConfig(value = {JmsClientTest.TestConfig.class})
@ContextConfiguration(initializers = {ArtemisContainerInitializer.class})
public class JmsClientTest {
    private final static Logger log = LoggerFactory.getLogger(JmsClientTest.class);

    @Configuration
    @Import(value = {JmsConfig.class})
    static class TestConfig {
    }

    @Autowired
    JmsClient jmsClient;

    @Test
    public void testSendAndReceive() {
        jmsClient.destination("test").send( "Hello");

        // wait to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> {
                    Optional<String> received = jmsClient.destination("test").receive(String.class);
                    assertThat(received.isPresent()).isTrue();

                    log.debug("Received message: {}", received.get());
                    assertThat(received.get()).isEqualTo("Hello");
                });
    }

    @Test
    public void testSendAndReceive_GreetingObject() {
        jmsClient.destination("testObject")
                .withTimeToLive(2_000)
                .withReceiveTimeout(1_000)
                .withPriority(1)
                .withDeliveryDelay(100)
                .withDeliveryPersistent(false)
                .send(new Greeting("Hello JmsClient!", Instant.now()));

        // wait to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> {
                    var received = jmsClient.destination("testObject")
                            .receive(Greeting.class);
                    assertThat(received).isPresent();

                    log.info("Greeting messages received: {}", received.get());
                    assertThat(received.get().body()).isEqualTo("Hello JmsClient!");
                });
    }
}
