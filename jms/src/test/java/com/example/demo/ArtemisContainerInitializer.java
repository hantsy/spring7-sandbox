package com.example.demo;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.activemq.ArtemisContainer;

import java.util.Map;

class ArtemisContainerInitializer implements ApplicationContextInitializer<@NotNull ConfigurableApplicationContext> {
    private static final Logger log = LoggerFactory.getLogger(ArtemisContainerInitializer.class);
    final static String DOCKER_IMAGE_NAME = "apache/activemq-artemis:latest-alpine";
    final static Integer DEFAULT_EXPOSED_PORT = 61616;
    final ArtemisContainer container = new ArtemisContainer(DOCKER_IMAGE_NAME)
            .withUser("user")
            .withPassword("password")
            .withExposedPorts(DEFAULT_EXPOSED_PORT);


    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        container.start();
        applicationContext.addApplicationListener((ContextClosedEvent e) -> container.stop());

        var brokerUrlFormat = "tcp://%s:%d";
        var brokerUrl = brokerUrlFormat.formatted(container.getHost(), container.getFirstMappedPort());
        log.debug("connection url is {}", brokerUrl);

        applicationContext.getEnvironment()
                .getPropertySources()
                .addLast(
                        new MapPropertySource("activemqProps",
                                Map.of("activemq.brokerUrl", brokerUrl)
                        )
                );
    }
}
