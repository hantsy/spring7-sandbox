package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ShareKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultShareConsumerFactory;
import org.springframework.kafka.core.ShareConsumerFactory;

import java.util.Map;

import static com.example.demo.DemoApplication.DEMO_GROUP_NAME;
import static com.example.demo.DemoApplication.DEMO_TOPIC_NAME;

@Configuration
@Slf4j
class ShareConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Bean
    NewTopic myTopic() {
        return new NewTopic(DEMO_TOPIC_NAME, 1, (short) 1);
    }

    @Bean
    public ShareConsumerFactory<String, String> shareConsumerFactory() {
        log.debug("Get bootstrap servers from properties:{}", bootstrapServers);
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, DEMO_GROUP_NAME,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
                //ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, "explicit"
        );
        DefaultShareConsumerFactory<String, String> factory = new DefaultShareConsumerFactory<>(props);
        factory.addListener(new ShareConsumerFactory.Listener<>() {
            @Override
            public void consumerAdded(String id, ShareConsumer<String, String> consumer) {
                log.debug("consumer added id:{}", id);
            }

            @Override
            public void consumerRemoved(@Nullable String id, ShareConsumer<String, String> consumer) {
                log.debug("consumer removed id:{}", id);
            }
        });
        return factory;
    }

    @Bean
    public ShareKafkaListenerContainerFactory<String, String> shareKafkaListenerContainerFactory(
            ShareConsumerFactory<String, String> shareConsumerFactory) {
        return new ShareKafkaListenerContainerFactory<>(shareConsumerFactory);
    }
}
