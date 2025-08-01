package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.example.demo.KafkaStreamsConfig.TOPIC_WORD_OUTPUT;

@Component
@Slf4j
public class WordCountListener {
    public List<String> messages = new ArrayList<>();

    @KafkaListener(groupId = "demo-group", topics = TOPIC_WORD_OUTPUT)
    public void countWord(ConsumerRecord<String, WordCount> record) {
        log.debug("<<<<<<<<<received record: {}->{}", record.key(), record.value());
        messages.add(record.key() + ":" + record.value().count());
    }
}
