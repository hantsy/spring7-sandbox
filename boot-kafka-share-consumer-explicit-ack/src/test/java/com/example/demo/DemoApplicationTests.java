package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.ShareGroupDescription;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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

    @Autowired
    private GreetingListener listener;

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
                        catch (ExecutionException ex) {
                            // The group does not exist until the first consumer joins; keep polling.
                            if (ex.getCause() instanceof GroupIdNotFoundException) {
                                throw new AssertionError("The share group is not available yet", ex);
                            }
                            throw new IllegalStateException("Failed to describe the share group", ex);
                        }
                        catch (TimeoutException ex) {
                            throw new AssertionError("Timed out describing the share group", ex);
                        }
                        catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted while describing the share group", ex);
                        }
                        var group = groups.get(DemoApplication.DEMO_GROUP_NAME);
                        assertThat(group).isNotNull();
                        assertThat(group.members())
                                .anyMatch(member -> member.assignment().topicPartitions().stream()
                                        .anyMatch(tp -> tp.topic().equals(DemoApplication.DEMO_TOPIC_NAME)));
                    });
        }
    }

    @Test
    public void testSendMessage() {
        awaitShareGroupAssignment();
        var sends = List.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog").stream()
                .map(word -> kafkaTemplate.send(DemoApplication.DEMO_TOPIC_NAME, UUID.randomUUID().toString(), word)
                        .thenAccept(s -> log.debug("sent message: {}", s)))
                .toList();
        CompletableFuture.allOf(sends.toArray(new CompletableFuture<?>[0])).join();

        // Every word is counted exactly once: the duplicate "the" is rejected, not redelivered.
        Awaitility.waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(this.listener.counter)
                        .containsOnly(
                                entry("the", 1L), entry("quick", 1L), entry("brown", 1L),
                                entry("fox", 1L), entry("jumps", 1L), entry("over", 1L),
                                entry("lazy", 1L), entry("dog", 1L)));
    }

}
