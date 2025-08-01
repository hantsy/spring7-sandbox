package com.example.demo;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
//@Import(TestcontainersConfiguration.class)
    @Testcontainers
class DemoApplicationTests {

    @Container
    static  RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"))
            .withExposedPorts(5672, 15672, 5552)
            .withEnv("RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS", "-rabbitmq_stream advertised_host localhost")
            .withCopyFileToContainer(MountableFile.forHostPath(Paths.get("rabbitmq/enable_plugins")), "/etc/rabbitmq/enable_plugins");

    @DynamicPropertySource
    static void dynamicPropertySource(final DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", () ->rabbitMQContainer.getMappedPort(5672));
        registry.add("spring.rabbitmq.stream.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.stream.port", () ->rabbitMQContainer.getMappedPort(5552));
//        registry.add("spring.rabbitmq.stream.username", rabbitMQContainer::getAdminUsername);
//        registry.add("spring.rabbitmq.stream.password", rabbitMQContainer::getAdminPassword);
    }

    @Autowired
    RabbitStreamTemplate rabbitStreamTemplate;

    @Autowired
    GreetingListener listener;

    @Test
    void testSendRabbitStream() {
        List.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
                .forEach(word -> rabbitStreamTemplate.convertAndSend(word));

        Awaitility.await().atMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> {
                    assertThat(listener.getWordCount("the")).isEqualTo(2);
                });
    }

}
