package com.example.demo;

import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListenerAnnotationBeanPostProcessor;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.SingleAmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.config.RabbitAmqpListenerContainerFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableRabbit
public class RabbitClientConfig {

    @Value("${rabbitmq.port:5672}")
    private int port;

    @Bean
    Environment environment() {
        return new AmqpEnvironmentBuilder()
                .connectionSettings()
                .port(port)
                .environmentBuilder()
                .build();
    }

    // The org.springframework.amqp.rabbit.connection.ConnectionFactory
    // is only for AMQP 0.9.1 protocol.
    @Bean
    AmqpConnectionFactory amqpConnectionFactory(Environment environment) {
        return new SingleAmqpConnectionFactory(environment);
    }

    @Bean
    RabbitAmqpAdmin rabbitAmqpAdmin(AmqpConnectionFactory connectionFactory) {
        return new RabbitAmqpAdmin(connectionFactory);
    }


    // The RabbitAmqpTemplate is an implementation of the AsyncAmqpTemplate
    // and performs various send/receive operations with AMQP 1.0 protocol.
    @Bean
    RabbitAmqpTemplate rabbitAmqpTemplate(AmqpConnectionFactory connectionFactory,
                                          MessageConverter jsonMessageConverter) {
        RabbitAmqpTemplate rabbitAmqpTemplate = new RabbitAmqpTemplate(connectionFactory);
        rabbitAmqpTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitAmqpTemplate;
    }

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    DirectExchange e1() {
        return new DirectExchange("e1");
    }

    @Bean
    Queue q1() {
        return QueueBuilder.durable("q1").deadLetterExchange("dlx1").build();
    }

    @Bean
    Binding b1() {
        return BindingBuilder.bind(q1()).to(e1()).with("k1");
    }

    @Bean(RabbitListenerAnnotationBeanPostProcessor.DEFAULT_RABBIT_LISTENER_CONTAINER_FACTORY_BEAN_NAME)
    RabbitAmqpListenerContainerFactory rabbitAmqpListenerContainerFactory(AmqpConnectionFactory connectionFactory) {
        RabbitAmqpListenerContainerFactory factory = new RabbitAmqpListenerContainerFactory(connectionFactory);
        return factory;
    }
}
