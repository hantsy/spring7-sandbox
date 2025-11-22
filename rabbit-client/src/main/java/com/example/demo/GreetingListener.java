package com.example.demo;

import com.rabbitmq.client.amqp.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Component
class GreetingListener {
    private static final Logger log = LoggerFactory.getLogger(GreetingListener.class);
//    final List<String> received = Collections.synchronizedList(new ArrayList<>());
//
//    CountDownLatch consumeIsDone = new CountDownLatch(11);

    private List<Greeting> messageList = new ArrayList<>();

    @Autowired
    JsonMapper jsonMapper;

    @RabbitListener(queues = {"q1"},
            //  ackMode = "#{T(org.springframework.amqp.core.AcknowledgeMode).MANUAL}",
            concurrency = "2",
            id = "testAmqpListener")
    void processQ1AndQ2Data(Message data) {
        //, AmqpAcknowledgment acknowledgment, Consumer.Context context) {
//        try {
//            if ("discard".equals(data)) {
//                if (!this.received.contains(data)) {
//                    context.discard();
//                } else {
//                    throw new MessageConversionException("Test message is rejected");
//                }
//            } else if ("requeue".equals(data) && !this.received.contains(data)) {
//                acknowledgment.acknowledge(AmqpAcknowledgment.Status.REQUEUE);
//            } else {
//                acknowledgment.acknowledge();
//            }
//            this.received.add(data);
//        } finally {
//            this.consumeIsDone.countDown();
//        }

        log.info("Received data from RabbitMQ: {}", data);
        Greeting greeting = jsonMapper.readValue(data.body(), Greeting.class);
        log.info("Converted message payload: {}", greeting);
        messageList.add(greeting);
    }

    public List<Greeting> getMessageList() {
        return messageList;
    }
}
