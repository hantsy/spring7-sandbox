package com.example.demo;

import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.OffsetSpecification;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.support.StreamAdmin;
import org.springframework.rabbit.stream.support.converter.DefaultStreamMessageConverter;
import org.springframework.rabbit.stream.support.converter.StreamMessageConverter;

import java.time.Duration;

@SpringBootApplication
public class DemoApplication {
    public static final String QUEUE_NAME = "demo-stream";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }


    @Bean
    StreamAdmin streamAdmin(Environment env) {
        return new StreamAdmin(env, sc -> {
            sc.stream(QUEUE_NAME).maxAge(Duration.ofHours(2)).create();
            sc.stream("test.stream.queue1").create();
            sc.stream("test.stream.queue2").create();
        });
    }

    @Bean
    RabbitListenerContainerFactory<StreamListenerContainer> rabbitListenerContainerFactory(Environment env) {
        return new StreamRabbitListenerContainerFactory(env);
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
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    StreamMessageConverter streamMessageConverter() {
        return new DefaultStreamMessageConverter();
    }
}

