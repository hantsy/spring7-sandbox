package com.example.demo;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.demo.RabbitClientConfig.HELLO_EXCHANGE_NAME;
import static com.example.demo.RabbitClientConfig.HELLO_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {
        HelloListenerContainerTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class HelloListenerContainerTest {
    private final Logger log = LoggerFactory.getLogger(HelloListenerContainerTest.class);

    @Configuration
    @Import({
            RabbitClientConfig.class,
            HelloListener.class
    })
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Autowired
    HelloListener listener;

    @Test
    void testHello() {
        log.debug("Start sending message...");
        var initialFuture = CompletableFuture.completedFuture(true);
        var words = List.of(
                "the",
                "quick",
                "dog",
                "jumped",
                "over",
                "the",
                "lazy",
                "fox");
        for (String word : words) {
            initialFuture = initialFuture
                    .thenComposeAsync(_ -> rabbitAmqpTemplate
                            .convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, word)
                            .whenComplete((res, ex) -> log.debug("sent: [{}]", word))
                    );
        }

        initialFuture.join();

        Awaitility.await().atMost(Duration.ofMillis(1_000))
                .untilAsserted(() -> {
                    List<String> received = this.listener.received;
                    log.debug(">>> ackListener received: {}", received);

                    Map<String, Long> wordCount = received.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                    log.debug("word count: {}", wordCount);

                    assertThat(wordCount.get("the")).isEqualTo(2);
                });

    }

}
