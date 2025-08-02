package com.example.demo;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

import static com.example.demo.DemoApplication.STEAM_NAME;

@Component
@RequiredArgsConstructor
@Slf4j
public class GreetingListener {
    private ConcurrentHashMap<String, Long> counter = new ConcurrentHashMap<>();

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    HashMapper hashMapper;

    //@EventListener(ApplicationReadyEvent.class)
    public void receiveMessage() {
        stringRedisTemplate.opsForStream()
                .read(Greeting.class,
                        Consumer.from("myGroup", "myConsumer"),
                        StreamReadOptions.empty().count(10).autoAcknowledge(),
                        StreamOffset.create(STEAM_NAME, ReadOffset.lastConsumed())
                )
                .stream()
                .map(record -> {
                    log.debug("receiving record id = {}, value = {}", record.getId(), record.getValue());
                    return record.getValue();
                })
                .forEach(record -> counter.compute(record.message(), (s, v) -> v == null ? 1 : v + 1));
    }

    public Long getWordCount(String word) {
        return this.counter.get(word);
    }
}
