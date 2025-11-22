package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
class GreetingListener {
    Map<String, Long> counter = new ConcurrentHashMap<>();

    @RabbitListener(queues = DemoApplication.QUEUE_NAME)
    public void onMessage(Greeting greeting) {
        log.debug("receiving message: {}", greeting);
        counter.compute(greeting.message(), (w, c) -> c == null ? 1 : c + 1);
    }

    public Long getWordCount(String word) {
        return counter.get(word);
    }
}
