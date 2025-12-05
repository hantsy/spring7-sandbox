package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.stream.Stream;

import static com.example.demo.DemoApplication.TOPIC_WORD_INPUT;
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
    private WordCountListener listener;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void testSendMessage() throws InterruptedException {
        Stream.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
                .forEach(word ->
                    kafkaTemplate.send(TOPIC_WORD_INPUT, word, word)
                            .thenAccept(w -> log.debug("sent message: " + w))
                            .join()
                );

        Awaitility.waitAtMost(Duration.ofMillis(10_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("THE:2"));
    }
}
