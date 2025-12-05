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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
        var futures = Stream.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
                .map(word ->
                        kafkaTemplate.send(TOPIC_WORD_INPUT, word, word)
                                .thenAccept(w -> log.debug("sent message: " + w))

                )
                .toArray(CompletableFuture[]::new);
        var latch = new CountDownLatch(1);
        CompletableFuture.allOf(futures)
                .whenComplete((unused, throwable) -> latch.countDown())
                .join();
        latch.await(10_000, TimeUnit.MILLISECONDS);

        Awaitility.waitAtMost(Duration.ofMillis(15_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("THE:2"));
    }
}
