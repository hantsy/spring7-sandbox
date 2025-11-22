package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
//@Import(TestcontainersConfiguration.class)
@Testcontainers
@Slf4j
class DemoApplicationTests {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"))
            .withExposedPorts(5672, 15672, 5552)
            .withEnv("RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS", "-rabbitmq_stream advertised_host localhost")
            .withCopyFileToContainer(MountableFile.forHostPath("./rabbitmq/enabled_plugins"), "/etc/rabbitmq/enabled_plugins")
            .withLogConsumer(outputFrame -> log.info("[Docker]>>>{}", outputFrame.getUtf8String()));

    @DynamicPropertySource
    static void dynamicPropertySource(final DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitMQContainer.getMappedPort(5672));
        registry.add("spring.rabbitmq.stream.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.stream.port", () -> rabbitMQContainer.getMappedPort(5552));
        registry.add("spring.rabbitmq.stream.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.stream.password", rabbitMQContainer::getAdminPassword);
    }

    @Autowired
    RabbitStreamTemplate rabbitStreamTemplate;

    @Autowired
    RabbitStreamTemplate offsetTrackRabbitStreamTemplate;

    @Autowired
    GreetingListener listener;

    @Autowired
    OffsetTrackListener offsetTrackListener;

    // @Autowired
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSendRabbitStream() {
        var streamsResult = Stream.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
                .map(word -> rabbitStreamTemplate.convertAndSend(Greeting.of(word)))
                .toArray(CompletableFuture[]::new);
//                .forEach(word -> rabbitStreamTemplate.convertAndSend(objectMapper.writeValueAsString(Greeting.of(word)), (Message m) -> {
//                            m.getMessageProperties().setType(Greeting.class.getTypeName());
//                            m.getMessageProperties().setContentType("application/json");
//                            return m;
//                        })
//                );

        CompletableFuture.allOf(streamsResult).join();

        Awaitility.await().atMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> {
                    assertThat(listener.getWordCount("the")).isEqualTo(2);
                });
    }

    @Test
    void testSendRabbitStream_offsetTrack() {
        var streamsResult = Stream.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
                .map(word -> offsetTrackRabbitStreamTemplate.convertAndSend(Greeting.of(word)))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(streamsResult).join();

        Awaitility.await().atMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> {
                    assertThat(offsetTrackListener.getWordCount("the")).isEqualTo(2);
                });
    }

}
