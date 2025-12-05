package com.example.demo;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.demo.DemoApplication.TOPIC_WORD_INPUT;
import static com.example.demo.DemoApplication.TOPIC_WORD_OUTPUT;

@Configuration
public class KafkaStreamsConfig {

    @Bean
    public  KStream<String, String> process(StreamsBuilder builder) {
        Serde<String> stringSerde = Serdes.String();
        Serde<Long> longSerde = Serdes.Long();

        KStream<String, String> wordStreams = builder
                .stream(TOPIC_WORD_INPUT, Consumed.with(stringSerde, stringSerde));

        KTable<String, Long> wordTable = wordStreams
                .mapValues(value -> value.toUpperCase())
                .groupBy((key, value) -> value, Grouped.with(stringSerde, stringSerde))
                .count(Materialized.as("wordCounts"));

        wordTable
                .toStream()
                .to(TOPIC_WORD_OUTPUT, Produced.with(stringSerde, longSerde));

        return wordStreams;
    }
}
