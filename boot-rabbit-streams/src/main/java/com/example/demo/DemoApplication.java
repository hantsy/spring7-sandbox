package com.example.demo;

import com.rabbitmq.stream.ByteCapacity;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.OffsetSpecification;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.rabbit.stream.support.StreamAdmin;

import java.time.Duration;

@SpringBootApplication
public class DemoApplication {
    public static final String QUEUE_NAME = "demo-stream";
    public static final String OFFSET_TRACK_STREAM = "test.stream.queue2";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }


    @Bean
    StreamAdmin streamAdmin(Environment env) {
        return new StreamAdmin(env, sc -> {
            sc.stream(QUEUE_NAME).maxAge(Duration.ofHours(2)).create();
            sc.stream(OFFSET_TRACK_STREAM).maxLengthBytes(ByteCapacity.GB(1)).create();
        });
    }


    @Bean
    RabbitListenerContainerFactory<StreamListenerContainer> nativeFactory(Environment env) {
        StreamRabbitListenerContainerFactory factory = new StreamRabbitListenerContainerFactory(env);
        factory.setNativeListener(true);
        factory.setConsumerCustomizer((id, builder) -> {
            builder.name("myConsumer")
                    .offset(OffsetSpecification.first())
                    .manualTrackingStrategy();
        });
        return factory;
    }

    @Bean
    RabbitStreamTemplate offsetTrackRabbitStreamTemplate(Environment env) {
        var template = new RabbitStreamTemplate(env, OFFSET_TRACK_STREAM);
        template.setMessageConverter(jsonMessageConverter());
        template.setProducerCustomizer((s, builder) -> builder
                .name("myProducer")
                .enqueueTimeout(Duration.ofSeconds(10))
                .confirmTimeout(Duration.ofSeconds(10)));
        return template;
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

