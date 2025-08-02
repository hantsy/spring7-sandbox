package com.example.demo;

import com.rabbitmq.stream.Message;
import com.rabbitmq.stream.MessageHandler.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Slf4j
public class OffsetTrackListener {
    Map<String, Long> counter = new ConcurrentHashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(id = "test", queues = DemoApplication.OFFSET_TRACK_STREAM, containerFactory = "nativeFactory")
    void nativeMsg(Message in, Context context) {
        Greeting greeting = objectMapper.readValue(in.getBodyAsBinary(), Greeting.class);
        log.debug("receiving message from test.stream.queue2: {}", greeting);
        context.storeOffset();
        counter.compute(greeting.message(), (w, c) -> c == null ? 1 : c + 1);
    }

    public Long getWordCount(String word) {
        return counter.get(word);
    }
}
