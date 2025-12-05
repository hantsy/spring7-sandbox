package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.1"));

    @DynamicPropertySource
    static void kafkaStreamsProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private Consumer listener;

    @Autowired
    private Producer producer;

    @Test
    public void testSendMessage() throws InterruptedException {
        producer.send(Flux.just("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"));
        Awaitility.waitAtMost(Duration.ofMillis(10_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("THE:2"));
    }
}
