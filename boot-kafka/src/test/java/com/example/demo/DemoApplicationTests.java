package com.example.demo;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private GreetingListener listener;

    @Test
    public void testSendMessage() {
        kafkaTemplate.send(DemoApplication.TOPIC_HELLO, Greeting.of("Hello Kafka"));
        Awaitility.waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("Hello Kafka"));
    }

}
