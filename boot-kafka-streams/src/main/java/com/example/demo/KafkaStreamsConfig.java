package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsCustomizer;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.time.Duration;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;

@Configuration(proxyBeanMethods = false)
@EnableKafkaStreams
@Slf4j
public class KafkaStreamsConfig {
    public static final String TOPIC_WORD_INPUT = "wordIn";
    public static final String TOPIC_WORD_OUTPUT = "wordOut";

    @Bean
    NewTopic wordInputTopic() {
        return TopicBuilder.name(TOPIC_WORD_INPUT).build();
    }

    @Bean
    NewTopic wordOutputTopic() {
        return TopicBuilder.name(TOPIC_WORD_OUTPUT).build();
    }

    @Bean
    public KStream<String, WordCount> wordCountStreams(StreamsBuilder builder) {
        KStream<String, String> streams = builder.stream(TOPIC_WORD_INPUT, Consumed.with(Serdes.String(), Serdes.String()));
        KStream<String, WordCount> wordCountStreams = streams.groupByKey()
                .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofMillis(1_000), Duration.ofMillis(50)))
                .count()
                .suppress(Suppressed.untilWindowCloses(unbounded()))
                .toStream()
                .map((key, value) -> {
                    System.out.println(">>>>>>>>>>>>> windowed key: " + key.key() + ", value: " + value);
                    return new KeyValue<>(key.key(), value);
                })
                .mapValues(WordCount::new);
        wordCountStreams
                .to(TOPIC_WORD_OUTPUT, Produced.with(Serdes.String(), new JacksonJsonSerde<>()));
        return wordCountStreams;
    }

    @Bean
    public KafkaStreamsCustomizer kafkaStreamsCustomizer() {
        return stream -> stream.setStateListener((newState, oldState) -> {
            log.debug("state: {} -> {}", oldState, newState);
        });
    }

//    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
//    public KafkaStreamsConfiguration kafkaStreamsConfiguration(KafkaProperties kafkaProperties) {
//        Map<String, Object> props = Map.of(
//                StreamsConfig.APPLICATION_ID_CONFIG, "demo",
//                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers(),
//                StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, StringSerde.class.getName(),
//                StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.IntegerSerde.class.getName()
//        );
//        return new KafkaStreamsConfiguration(props);
//    }

//    @Bean
//    public KafkaStreamsInfrastructureCustomizer kafkaStreamsInfrastructureCustomizer() {
//       return new  KafkaStreamsInfrastructureCustomizer(){
//           @Override
//           public void configureBuilder(StreamsBuilder builder) {
//           }
//
//           @Override
//           public void configureTopology(Topology topology) {
//           }
//       };
//    }
}
