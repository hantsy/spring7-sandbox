package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.demo.DemoApplication.DEMO_GROUP_NAME;
import static com.example.demo.DemoApplication.DEMO_TOPIC_NAME;

@Component
@Slf4j
public class GreetingListener {
    public Map<String, Long> counter = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = DEMO_TOPIC_NAME,
            containerFactory = "shareKafkaListenerContainerFactory",
            groupId = DEMO_GROUP_NAME
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.debug("received record: {} at {}", record, LocalDateTime.now());
        counter.compute(record.value(), (s, v) -> v == null ? 1 : v + 1);
    }

    public Long getWordCount(String word) {
        return this.counter.get(word);
    }
}
