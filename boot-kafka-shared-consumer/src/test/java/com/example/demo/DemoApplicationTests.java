package com.example.demo;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

//@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

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
