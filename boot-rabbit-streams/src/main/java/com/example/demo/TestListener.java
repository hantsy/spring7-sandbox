package com.example.demo;

import com.rabbitmq.stream.Message;
import com.rabbitmq.stream.MessageHandler;
import com.rabbitmq.stream.MessageHandler.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class TestListener {

    @RabbitListener(queues = "test.stream.queue1")
    void listen(String in) {
        log.debug("receiving message from test.stream.queue1: {}", in);
    }

    @RabbitListener(id = "test", queues = "test.stream.queue2", containerFactory = "nativeFactory")
    void nativeMsg(Message in, Context context) {
        log.debug("receiving message from test.stream.queue2: {}", in);
        context.storeOffset();
    }
}
