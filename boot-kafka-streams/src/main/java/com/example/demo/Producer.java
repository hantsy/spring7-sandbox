package com.example.demo;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static com.example.demo.DemoApplication.TOPIC_WORD_INPUT;

@Component
@RequiredArgsConstructor
public class Producer {

    private static Logger log = LoggerFactory.getLogger(Producer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    @SneakyThrows
    @EventListener(ApplicationReadyEvent.class)
    public void send() {
        var inputs = Flux.just("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog");
        var intervals = Flux.interval(Duration.ofMillis(1_000), Duration.ofMillis(500));
        Flux.zip(intervals, inputs)
                .log()
                .map(it -> kafkaTemplate.send(TOPIC_WORD_INPUT, it.getT2(), it.getT2()))
                .blockLast();
    }
}
