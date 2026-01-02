package com.example.demo;

import com.rabbitmq.client.amqp.Consumer;
import com.rabbitmq.client.amqp.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAcknowledgment;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.example.demo.RabbitClientConfig.HELLO_QUEUE_NAME;

@Component
class AckListener {
    private static final Logger log = LoggerFactory.getLogger(AckListener.class);
    final List<String> received = Collections.synchronizedList(new ArrayList<>());

    CountDownLatch consumeIsDone = new CountDownLatch(11);

    @RabbitListener(queues = {HELLO_QUEUE_NAME},
              ackMode = "#{T(org.springframework.amqp.core.AcknowledgeMode).MANUAL}",
            concurrency = "2",
            id = "testAmqpListener")
    void processAckManually(Message message, AmqpAcknowledgment acknowledgment, Consumer.Context context) {
        var data = new String(message.body());
        System.out.println("received: " + data);
        log.debug(":: received data: [{}]", data);
        try {
            if ("discard".equals(data)) {
                if (!this.received.contains(data)) {
                    log.debug(":: ack with discard");
                    context.discard();
                } else {
                    log.debug(":: throw new MessageConversionException");
                    throw new MessageConversionException("Test message is rejected");
                }
            } else if ("requeue".equals(data) && !this.received.contains(data)) {
                log.debug(":: ack with requeue");
                acknowledgment.acknowledge(AmqpAcknowledgment.Status.REQUEUE);
            } else {
                log.debug(":: ack with accept");
                acknowledgment.acknowledge();
            }
            this.received.add(data);
            log.debug(":: current received:{}", this.received);
        } finally {
            this.consumeIsDone.countDown();
        }
    }
}
