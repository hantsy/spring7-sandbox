package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.client.AmqpClient;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private AmqpClient amqpClient;

    @Autowired
    private GreetingListener listener;

    @Test
    public void testSendMessage() {
        amqpClient.sendToDefault(new Message("Hello AMQP".getBytes())).join();

        waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("Hello AMQP"));
    }

}
