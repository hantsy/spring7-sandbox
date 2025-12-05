package com.example.demo;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsCustomizer;
import org.springframework.kafka.config.KafkaStreamsInfrastructureCustomizer;
import org.springframework.kafka.config.TopicBuilder;

@SpringBootApplication
@EnableKafkaStreams
public class DemoApplication {
    private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    public static final String TOPIC_WORD_INPUT = "word-in";
    public static final String TOPIC_WORD_OUTPUT = "word-out";

    @Bean
    NewTopic wordInputTopic() {
        return TopicBuilder.name(TOPIC_WORD_INPUT)
                .partitions(3)
                //.replicas(3)
                .build();
    }

    @Bean
    NewTopic wordOutputTopic() {
        return TopicBuilder.name(TOPIC_WORD_OUTPUT)
                .partitions(3)
                //.replicas(3)
                .build();
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
//                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", kafkaProperties.getBootstrapServers()),
//                StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName(),
//                StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName()
//        );
//        return new KafkaStreamsConfiguration(props);
//    }

    @Bean
    public KafkaStreamsInfrastructureCustomizer kafkaStreamsInfrastructureCustomizer() {
        return new KafkaStreamsInfrastructureCustomizer() {
            @Override
            public void configureBuilder(StreamsBuilder builder) {
                log.debug("configuring streams builder...");
            }

            @Override
            public void configureTopology(Topology topology) {
                log.debug("configuring topology...");
            }
        };
    }
}
