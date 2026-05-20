package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.ShareAcknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.demo.DemoApplication.DEMO_GROUP_EXPLICIT_NAME;
import static com.example.demo.DemoApplication.DEMO_TOPIC_EXPLICIT_NAME;

@Component
@Slf4j
public class ExplicitGreetingListener {
    public Map<String, Long> counter = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = DEMO_TOPIC_EXPLICIT_NAME,
            containerFactory = "explicitShareKafkaListenerContainerFactory",
            groupId = DEMO_GROUP_EXPLICIT_NAME
    )
    public void onMessage(ConsumerRecord<String, String> record, ShareAcknowledgment acknowledgment) {
        log.debug("received record: {} at {}", record, LocalDateTime.now());
        counter.compute(record.value(), (s, v) -> {
                    if (v == null) {
                        acknowledgment.acknowledge();
                        return 1L;
                    } else {
                        acknowledgment.reject(); // reject when the word is tapped.
                        return v;
                    }
                }
        );
    }

    public Long getWordCount(String word) {
        return this.counter.get(word);
    }
}
