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
class DemoApplicationTests {
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
