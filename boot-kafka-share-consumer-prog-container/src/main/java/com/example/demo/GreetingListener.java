package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class GreetingListener implements MessageListener<String, String> {

    public final Map<String, Integer> counter = new ConcurrentHashMap<>();

    @Override
    public void onMessage(ConsumerRecord<String, String> record) {
        log.debug("received message: {}", record);
        counter.compute(record.value(), (s, v) -> v == null ? 1 : ++v);
    }
}
