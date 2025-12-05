package com.example.demo;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.example.demo.DemoApplication.TOPIC_WORD_INPUT;
import static com.example.demo.DemoApplication.TOPIC_WORD_OUTPUT;

@Component
public class Processor {

    @Autowired
    public void process(StreamsBuilder builder) {
        Serde<String> stringSerde = Serdes.String();
        Serde<Long> longSerde = Serdes.Long();

        KStream<String, String> wordStreamsInput = builder
                .stream(TOPIC_WORD_INPUT, Consumed.with(stringSerde, stringSerde));

        KTable<String, Long> wordStreamsOutput = wordStreamsInput
                .mapValues(value -> value.toUpperCase())
                .groupBy((key, value) -> value, Grouped.with(stringSerde, stringSerde))
                .count(Materialized.as("wordCounts"));

        wordStreamsOutput
                .toStream()
                .to(TOPIC_WORD_OUTPUT, Produced.with(stringSerde, longSerde));
    }
}
