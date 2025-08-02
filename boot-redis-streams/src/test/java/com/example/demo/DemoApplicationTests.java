package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.hash.HashMapper;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.stream.Stream;

import static com.example.demo.DemoApplication.STEAM_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    HashMapper hashMapper;

    @Autowired
    private GreetingListener listener;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void sendMessage() {
        var createdGroup = stringRedisTemplate.opsForStream(hashMapper).createGroup("demo", "myGroup");
        log.debug("created group: {}", createdGroup);
        Stream.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
                .forEach(word -> {
                    var record = StreamRecords.newRecord()
                            .ofObject(Greeting.of(word))
                            .withStreamKey(STEAM_NAME);
                    var id = stringRedisTemplate.opsForStream()
                            .add(record);

                    log.debug("added to stream: {}", id);
                });

        listener.receiveMessage();

        Awaitility.await().atMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> {
                    assertThat(listener.getWordCount("the")).isEqualTo(2);
                });
    }

}
