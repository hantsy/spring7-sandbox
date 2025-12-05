package com.example.demo;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static com.example.demo.DemoApplication.TOPIC_WORD_INPUT;
import static com.example.demo.DemoApplication.TOPIC_WORD_OUTPUT;

@Configuration
public class KafkaStreamsConfig {

    @Bean
    public KStream<String, String> process(StreamsBuilder builder) {
        Serde<String> stringSerde = Serdes.String();
        Serde<Long> longSerde = Serdes.Long();

        KStream<String, String> wordStreams = builder
                .stream(TOPIC_WORD_INPUT, Consumed.with(stringSerde, stringSerde));

        wordStreams
                .mapValues(value -> value.toUpperCase())
                .groupBy((key, value) -> value, Grouped.with(stringSerde, stringSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMillis(1_000)))
                .count()
                .toStream()
                .map((key, value) -> new KeyValue<>(key.key(), value))
                .to(TOPIC_WORD_OUTPUT, Produced.with(stringSerde, longSerde));

        return wordStreams;
    }
}
