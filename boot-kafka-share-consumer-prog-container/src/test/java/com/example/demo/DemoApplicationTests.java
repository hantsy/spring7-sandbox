package com.example.demo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.ShareGroupDescription;
import org.apache.kafka.common.errors.TopicExistsException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

//@Import(TestcontainersConfiguration.class)
@Testcontainers
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    // Kafka 4.2 enabled share consumer by default
    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:4.4.0"))
            .withEnv("KAFKA_SHARE_COORDINATOR_STATE_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_SHARE_COORDINATOR_STATE_TOPIC_MIN_ISR", "1");

    @BeforeAll
    static void createTopic() throws Exception {
        // The share group has to know the topic before it is formed, otherwise its first
        // assignment is empty and the records produced in the meantime are skipped.
        try (var admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()))) {
            try {
                admin.createTopics(List.of(new NewTopic(DemoApplication.DEMO_TOPIC_NAME, 1, (short) 1)))
                        .all().get(30, TimeUnit.SECONDS);
            }
            catch (ExecutionException ex) {
                if (!(ex.getCause() instanceof TopicExistsException)) {
                    throw ex;
                }
            }
        }
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static void awaitShareGroupAssignment() {
        try (var admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()))) {
            Awaitility.waitAtMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        Map<String, ShareGroupDescription> groups;
                        try {
                            groups = admin.describeShareGroups(List.of(DemoApplication.DEMO_GROUP_NAME))
                                    .all().get(10, TimeUnit.SECONDS);
                        }
                        catch (Exception ex) {
                            throw new AssertionError("The share group is not available yet", ex);
                        }
                        var group = groups.get(DemoApplication.DEMO_GROUP_NAME);
                        assertThat(group).isNotNull();
                        assertThat(group.members()).isNotEmpty();
                        assertThat(group.members().iterator().next().assignment().topicPartitions())
                                .isNotEmpty();
                    });
        }
    }

    @SneakyThrows
    @Test
    public void testSendMessage() {
        awaitShareGroupAssignment();
        List.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
                .forEach(word -> kafkaTemplate.send(DemoApplication.DEMO_TOPIC_NAME, UUID.randomUUID().toString(), word)
                        .thenAccept(s -> log.debug("sent message: {}", s)));

        Awaitility.waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(GreetingListener.counter.get("the")).isEqualTo(2));
    }

}
