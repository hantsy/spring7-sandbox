package com.example.demo;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ShareKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ShareKafkaMessageListenerContainer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.demo.DemoApplication.SHARED_GROUP_NAME;

@Configuration
@Slf4j
@EnableKafka
class ShareConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @PostConstruct
    private void configureShareGroup() throws Exception {
        Map<String, Object> adminProps = new HashMap<>();
        adminProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (Admin admin = Admin.create(adminProps)) {
            ConfigResource configResource = new ConfigResource(ConfigResource.Type.GROUP, SHARED_GROUP_NAME);
            ConfigEntry configEntry = new ConfigEntry("share.auto.offset.reset", "earliest");

            Map<ConfigResource, Collection<AlterConfigOp>> configs = Map.of(
                    configResource, List.of(new AlterConfigOp(configEntry, AlterConfigOp.OpType.SET))
            );

            admin.incrementalAlterConfigs(configs).all().get();

            admin.createTopics(List.of(new NewTopic(DemoApplication.SHARED_TOPIC_NAME, 1, (short) 1)));
        }
    }

//    @Bean
//    NewTopic shareTopic() {
//        return new NewTopic(DemoApplication.SHARED_TOPIC_NAME, 1, (short) 1);
//    }

    @Bean
    public ProducerFactory<String, Object> shareProducerFactory() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
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
    public ShareConsumerFactory<String, Object> shareConsumerFactory() {
        log.debug("Get bootstrap servers from properties:{}", bootstrapServers);
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, SHARED_GROUP_NAME,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
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
    public ShareKafkaListenerContainerFactory<String, Object> shareKafkaListenerContainerFactory(
            ShareConsumerFactory<String, Object> shareConsumerFactory) {
        return new ShareKafkaListenerContainerFactory<>(shareConsumerFactory);
    }
}
