package com.example.demo;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsClient;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;

@Configuration
@EnableJms
public class JmsConfig {

    @Autowired
    Environment environment;

    @Bean
    public CachingConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(
                new ActiveMQConnectionFactory(environment.getProperty("activemq.brokerUrl"), "user", "password")
        );
    }

    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter messageConverter = new JacksonJsonMessageConverter();
        messageConverter.setTypeIdPropertyName("_type");
        return messageConverter;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setMessageConverter(messageConverter());
        return factory;
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory());
        jmsTemplate.setMessageConverter(messageConverter());
        return jmsTemplate;
    }

    @Bean
    public JmsClient jmsClient() {
        return JmsClient.create(jmsTemplate());
    }
}
