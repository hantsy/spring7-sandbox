package com.example.demo;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static com.example.demo.KafkaStreamsConfig.TOPIC_WORD_INPUT;
import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private WordCountListener listener;


    @Test
    public void testSendMessage() {
        List.of("the", "quick", "brown", "fox", "jumps", "over","the", "lazy", "dog")
                        .forEach(word -> kafkaTemplate.send(TOPIC_WORD_INPUT, word, word));
        Awaitility.waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("the:2"));
    }
}
