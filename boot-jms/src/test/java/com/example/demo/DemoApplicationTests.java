package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsClient;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.waitAtMost;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    GreetingListener listener;

    @Autowired
    JmsClient jmsClient;

    @Autowired
    JmsMessagingTemplate jmsMessagingTemplate;

    @Autowired
    JmsTemplate jmsTemplate;

    @Test
    public void testSendMessage() {
        jmsClient.destination(DemoApplication.DESTENATION_HELLO).send(Greeting.of("Hello World"));
        waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.latestMessage).isEqualTo("Hello World"));
    }

    @Test
    void testSendAndReceive2_withJmsClient() {
        String testMsg = "Hello World 2";
        jmsClient.destination("test").withTimeToLive(1_000).send(testMsg);

        var received = jmsClient.destination("test").withReceiveTimeout(1_500).receive(String.class);

        assertThat(received.isPresent()).isTrue();
        assertThat(received.get()).isEqualTo(testMsg);
    }

    @Test
    void testSendAndReceive2_withJmsClient_Greeting() {
        var testMsg = Greeting.of("Hello World 2");
        jmsClient.destination("test").withTimeToLive(1_000).send(testMsg);

        var received = jmsClient.destination("test").withReceiveTimeout(1_500).receive(Greeting.class);

        assertThat(received.isPresent()).isTrue();
        assertThat(received.get()).isEqualTo(testMsg);
    }

    @Test
    void testSendAndReceive2_withJsmTemplate_Greeting() {
        var testMsg = Greeting.of("Hello World");
        jmsTemplate.convertAndSend(DemoApplication.DESTENATION_HELLO, testMsg);

        waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.latestMessage).isEqualTo("Hello World"));
    }

    @Test
    void testSendAndReceive3_withJsmMessagingTemplate() {
        String testMsg = "Hello World 3";
        jmsMessagingTemplate.convertAndSend("test", testMsg);

        await().atMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> {
                    var receivedTestMsg = jmsMessagingTemplate.receiveAndConvert("test", String.class);
                    assertThat(receivedTestMsg).isEqualTo(testMsg);
                });
    }

    @Test
    void testSendAndReceive3_withJsmMessagingTemplate_Greeting() {
        var testMsg = Greeting.of("Hello World 3");
        jmsMessagingTemplate.convertAndSend("test", testMsg);

        await().atMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> {
                    var receivedTestMsg = jmsMessagingTemplate.receiveAndConvert("test", Greeting.class);
                    assertThat(receivedTestMsg).isEqualTo(testMsg);
                });
    }

}
