package com.example.demo;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

class RabbitContainerInitializer implements ApplicationContextInitializer<@NotNull ConfigurableApplicationContext> {
    private static final Logger log = LoggerFactory.getLogger(RabbitContainerInitializer.class);
    final static String DOCKER_IMAGE_NAME = "rabbitmq:4-management-alpine";
    final RabbitMQContainer container = new RabbitMQContainer(DockerImageName.parse(DOCKER_IMAGE_NAME))
            .withExposedPorts(5672, 15672, 5552);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        container.start();
        applicationContext.addApplicationListener(e -> {
            if (e instanceof ContextClosedEvent) {
                container.stop();
            }
        });
        log.debug("RabbitMQ container exposed ports:" + container.getFirstMappedPort());
        applicationContext.getEnvironment()
                .getPropertySources()
                .addLast(
                        new MapPropertySource("rabbitProps",
                                Map.of("rabbitmq.port", container.getFirstMappedPort())
                        )
                );
    }
}
