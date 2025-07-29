package com.example.demo;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.core.PulsarTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private PulsarTemplate<@NotNull Greeting> pulsarTemplate;

    @Autowired
    private GreetingListener listener;

    @Test
    public void testSendMessage() {
        pulsarTemplate.send(DemoApplication.TOPIC_HELLO,  Greeting.of("Hello Pulsar"));
        waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("Hello Pulsar"));
    }

}
