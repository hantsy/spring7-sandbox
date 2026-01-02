package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.demo.RabbitClientConfig.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {
        RabbitAmqpTemplateTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class RabbitAmqpTemplateTest {

    @Configuration
    @Import({RabbitClientConfig.class})
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Test
    void testSendAndReceive() throws Exception {
        assertThat(this.rabbitAmqpTemplate.convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, "test1"))
                .succeedsWithin(Duration.ofSeconds(10));

        assertThat(this.rabbitAmqpTemplate.receiveAndConvert(HELLO_QUEUE_NAME))
                .succeedsWithin(Duration.ofSeconds(10))
                .isEqualTo("test1");
    }

    @Test
    void verifyRpc() {
        String testRequest = "rpc-request";
        String testReply = "rpc-reply";

        CompletableFuture<Object> rpcClientResult = this.rabbitAmqpTemplate.convertSendAndReceive("e1", "k1", testRequest);

        AtomicReference<String> receivedRequest = new AtomicReference<>();
        CompletableFuture<Boolean> rpcServerResult =
                this.rabbitAmqpTemplate.<String, String>receiveAndReply("q1",
                        payload -> {
                            receivedRequest.set(payload);
                            return testReply;
                        });

        assertThat(rpcServerResult).succeedsWithin(Duration.ofSeconds(10)).isEqualTo(true);
        assertThat(rpcClientResult).succeedsWithin(Duration.ofSeconds(10)).isEqualTo(testReply);
        assertThat(receivedRequest.get()).isEqualTo(testRequest);
    }
}
