package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
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
    private AmqpTemplate amqpTemplate;

    @Autowired
    private GreetingListener listener;

    @Test
    public void testSendMessage() {
        amqpTemplate.convertAndSend(
                DemoApplication.EXCHANGE_HELLO,
                DemoApplication.ROUTING_HELLO,
                Greeting.of("Hello Rabbit")
        );

        waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("Hello Rabbit"));
    }

}
