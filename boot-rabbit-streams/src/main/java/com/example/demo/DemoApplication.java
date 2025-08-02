package com.example.demo;

import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.OffsetSpecification;
import com.rabbitmq.stream.ProducerBuilder;
import com.rabbitmq.stream.codec.SimpleCodec;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.amqp.autoconfigure.RabbitStreamTemplateConfigurer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.producer.ProducerCustomizer;
import org.springframework.rabbit.stream.support.StreamAdmin;
import org.springframework.rabbit.stream.support.StreamMessageProperties;
import org.springframework.rabbit.stream.support.converter.DefaultStreamMessageConverter;
import org.springframework.rabbit.stream.support.converter.StreamMessageConverter;

import java.time.Duration;
import java.util.Random;

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

//    @Bean
//    RabbitListenerContainerFactory<StreamListenerContainer> rabbitListenerContainerFactory(Environment env) {
//        StreamRabbitListenerContainerFactory factory = new StreamRabbitListenerContainerFactory(env);
//        factory.setContainerCustomizer(container ->
//                container.setStreamConverter(streamMessageConverter()));
//
//        return factory;
//    }

    @Bean
    RabbitListenerContainerFactory<StreamListenerContainer> nativeFactory(Environment env) {
        StreamRabbitListenerContainerFactory factory = new StreamRabbitListenerContainerFactory(env);
        factory.setNativeListener(true);
        //factory.setContainerCustomizer(container -> container.setStreamConverter(streamMessageConverter()));
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

//    @Bean
//    StreamMessageConverter streamMessageConverter() {
//        return new DefaultStreamMessageConverter();
//    }

//    @Bean
//    ProducerCustomizer producerCustomizer() {
//        return (name, builder) -> builder.name("producer-"+ new Random().nextLong())
//                .stream(QUEUE_NAME)
//                .enqueueTimeout(Duration.ofMillis(5_000))
//                .confirmTimeout(Duration.ofMillis(5_000));
//    }
//
//    @Bean
//    RabbitStreamTemplateConfigurer rabbitStreamTemplateConfigurer() {
//         var configurer = new RabbitStreamTemplateConfigurer();
//         configurer.setProducerCustomizer(producerCustomizer());
//         configurer.setMessageConverter(jsonMessageConverter());
//         //configurer.setStreamMessageConverter(streamMessageConverter());
//         return configurer;
//    }
}

