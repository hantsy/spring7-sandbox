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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.example.demo.RabbitClientConfig.HELLO_EXCHANGE_NAME;
import static com.example.demo.RabbitClientConfig.HELLO_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {
        AckListenerContainerTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class AckListenerContainerTest {
    private final Logger log = LoggerFactory.getLogger(AckListenerContainerTest.class);

    @Configuration
    @Import({
            RabbitClientConfig.class,
            AckListener.class
    })
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Autowired
    AckListener ackListener;

    @Test
    void testAck() {
        log.debug("Start sending message...");
        var initialFuture = CompletableFuture.completedFuture(true);
        var wordSupplier = List.<Supplier<String>>of(
                () -> "the",
                () -> "discard",
                () -> "quick",
                () -> "dog",
                () -> "jumped",
                () -> "over",
                () -> "the",
                () -> "requeue",
                () -> "lazy",
                () -> "discard",
                () -> "fox");
        for (Supplier<String> word : wordSupplier) {
            var s = word.get();
            initialFuture = initialFuture.thenComposeAsync(_ -> rabbitAmqpTemplate.convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, s)
                    .whenComplete((res, ex) -> {
                        log.debug("sent: [{}]", s);
                    }));
        }

        initialFuture.join();

        Awaitility.await().atMost(Duration.ofMillis(1_5000))
                .untilAsserted(() -> {
                    List<String> received = this.ackListener.received;
                    log.debug(">>> ackListener received: {}", received);

                    Map<String, Long> wordCount = received.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                    log.debug("word count: {}", wordCount);

                    assertThat(wordCount.get("discard")).isEqualTo(1);
                    assertThat(wordCount.get("the")).isEqualTo(2);
                });

    }

}
