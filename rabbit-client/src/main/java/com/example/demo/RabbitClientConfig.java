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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitClientConfig {
    public final static String HELLO_EXCHANGE_NAME = "e1";
    public final static String HELLO_QUEUE_NAME = "q1";
    public final static String HELLO_ROUTING_KEY = "k1";

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
    // The new AmqpConnectionFactory is for AMQP 1.0
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
    RabbitAmqpTemplate rabbitAmqpTemplate(AmqpConnectionFactory connectionFactory) {
        RabbitAmqpTemplate rabbitAmqpTemplate = new RabbitAmqpTemplate(connectionFactory);
        return rabbitAmqpTemplate;
    }

    @Bean
    DirectExchange helloExchange() {
        return ExchangeBuilder
                .directExchange(HELLO_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    Queue helloQueue() {
        return QueueBuilder.durable(HELLO_QUEUE_NAME)
                .deadLetterExchange("dlx1")
                .build();
    }

    @Bean
    Binding helloBinding() {
        return BindingBuilder
                .bind(helloQueue())
                .to(helloExchange())
                .with(HELLO_ROUTING_KEY);
    }

    @Bean(RabbitListenerAnnotationBeanPostProcessor.DEFAULT_RABBIT_LISTENER_CONTAINER_FACTORY_BEAN_NAME)
    RabbitAmqpListenerContainerFactory rabbitAmqpListenerContainerFactory(AmqpConnectionFactory connectionFactory) {
        RabbitAmqpListenerContainerFactory factory = new RabbitAmqpListenerContainerFactory(connectionFactory);
        return factory;
    }
}
