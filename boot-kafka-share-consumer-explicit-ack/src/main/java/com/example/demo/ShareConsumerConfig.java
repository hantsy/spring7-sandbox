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

import static com.example.demo.DemoApplication.DEMO_TOPIC_NAME;

@Configuration
@Slf4j
class ShareConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Bean
    NewTopic demoExplicitTopic() {
        return new NewTopic(DEMO_TOPIC_NAME, 1, (short) 1);
    }

    /// //////////////////// explicit acknowledge ///////////////////////////
    @Bean
    public ShareConsumerFactory<String, String> explicitShareConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, "explicit"
        );
        return new DefaultShareConsumerFactory<>(props);
    }

    @Bean
    public ShareKafkaListenerContainerFactory<String, String> explicitShareKafkaListenerContainerFactory(
            ShareConsumerFactory<String, String> explicitShareConsumerFactory) {
        var factory = new ShareKafkaListenerContainerFactory<>(explicitShareConsumerFactory);
        //  factory.setConcurrency(10);
        return factory;
    }

}
