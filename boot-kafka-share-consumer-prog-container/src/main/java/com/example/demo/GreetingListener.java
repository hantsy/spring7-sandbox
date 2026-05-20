package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GreetingListener implements MessageListener<String, String> {

    public static ConcurrentHashMap<String, Integer> counter = new ConcurrentHashMap<>();

    @Override
    public void onMessage(ConsumerRecord<String, String> record) {
        log.debug("received message: {}", record);
        counter.compute(record.value(), (s, v) -> v == null ? 1 : ++v);
    }
}
