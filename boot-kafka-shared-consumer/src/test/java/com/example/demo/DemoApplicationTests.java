package com.example.demo;

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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

//@Import(TestcontainersConfiguration.class)
@Testcontainers
@SpringBootTest
class DemoApplicationTests {
    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.1"))
            .withEnv(
                    Map.of(
                            "KAFKA_UNSTABLE_API_VERSIONS_ENABLE", "true",
                            "KAFKA_GROUP_COORDINATOR_REBALANCE_PROTOCOLS", "classic,consumer,share",
                            "KAFKA_SHARE_COORDINATOR_STATE_TOPIC_REPLICATION_FACTOR", "1",
                            "KAFKA_SHARE_COORDINATOR_STATE_TOPIC_MIN_ISR", "1"
                    )
            )
            // enable kafka share group
           .withCommand("/opt/kafka/bin/kafka-features.sh --bootstrap-server 0.0.0.0:9092 upgrade --feature share.version=1");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, Object> shareKafkaTemplate;

    @Autowired
    private GreetingListener listener;

    @Test
    public void testSendMessage() {
        var sendResults = Stream.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
                .map(word ->
                        shareKafkaTemplate.send(DemoApplication.SHARED_TOPIC_NAME, Greeting.of(word))
                )
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(sendResults).join();

        Awaitility.waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.getWordCount("the")).isEqualTo(2));
    }

}
