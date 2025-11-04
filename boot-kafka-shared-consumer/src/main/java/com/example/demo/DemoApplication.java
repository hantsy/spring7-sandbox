package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ShareKafkaMessageListenerContainer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    public final static String SHARED_TOPIC_NAME = "my-topic";
    public static final String SHARED_GROUP_NAME = "my-share-group";
}

@Configuration
@Slf4j
//@EnableKafka
class ShareConsumerConfig {

//    @Bean
//    NewTopic shareTopic() {
//        return new NewTopic(DemoApplication.SHARED_TOPIC_NAME, 1, (short) 1);
//    }

    @Bean
    public ProducerFactory<String, Object> shareProducerFactory(KafkaProperties properties) {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers().getFirst(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class
        );

        DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(props);
        producerFactory.addListener(new ProducerFactory.Listener<>() {

            @Override
            public void producerAdded(String id, Producer<String, Object> producer) {
                log.debug("producer added: {}", id);
            }

            @Override
            public void producerRemoved(String id, Producer<String, Object> producer) {
                log.debug("producer removed: {}", id);
            }
        });
        return producerFactory;
    }

    @Bean
    KafkaTemplate<String, Object> shareKafkaTemplate(ProducerFactory<String, Object> shareProducerFactory) {
        return new KafkaTemplate(shareProducerFactory);
    }

    @Bean
    public ShareConsumerFactory<String, Object> shareConsumerFactory(KafkaProperties properties) {
        log.debug("Get bootstrap servers from properties:{}", properties.getBootstrapServers());
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers().getFirst(),
                ConsumerConfig.GROUP_ID_CONFIG, DemoApplication.SHARED_GROUP_NAME,
                ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 1000,
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30_000,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        DefaultShareConsumerFactory<String, Object> factory = new DefaultShareConsumerFactory<>(props);
        factory.addListener(new ShareConsumerFactory.Listener<>() {
            @Override
            public void consumerAdded(String id, ShareConsumer<String, Object> consumer) {
                log.debug("consumer added id:{}", id);
            }

            @Override
            public void consumerRemoved(@Nullable String id, ShareConsumer<String, Object> consumer) {
                log.debug("consumer removed id:{}", id);
            }
        });
        return factory;
    }

    @Bean
    public ShareKafkaMessageListenerContainer<String, Object> container(
            ShareConsumerFactory<String, Object> shareConsumerFactory, GreetingListener listener) {

        ContainerProperties containerProps = new ContainerProperties(DemoApplication.SHARED_TOPIC_NAME);
        containerProps.setGroupId(DemoApplication.SHARED_GROUP_NAME);

        ShareKafkaMessageListenerContainer<String, Object> container =
                new ShareKafkaMessageListenerContainer<>(shareConsumerFactory, containerProps);

        container.setupMessageListener(listener);

        return container;
    }
}