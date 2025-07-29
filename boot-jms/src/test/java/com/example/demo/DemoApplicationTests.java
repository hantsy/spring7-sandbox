package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    Sender sender;

    @Autowired
    Receiver receiver;

    @Autowired
    JmsClient jmsClient;

    @Test
    void testSendAndReceive() {
        String testMsg = "Hello World";
        sender.sendMessage(testMsg);

        await().atMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> {
                    assertThat(receiver.latestMessage()).isEqualTo(testMsg);
                });
    }

    @Test
    void testSendAndReceive2() {
        String testMsg = "Hello World 2";
        jmsClient.destination("test").withTimeToLive(1_000).send(testMsg);

        var received = jmsClient.destination("test").withReceiveTimeout(1_500).receive(String.class);

        assertThat(received.isPresent()).isTrue();
        assertThat(received.get()).isEqualTo(testMsg);
    }

}
