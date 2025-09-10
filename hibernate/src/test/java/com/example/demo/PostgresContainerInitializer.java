package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

class PostgresContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private final static Logger log = LoggerFactory.getLogger(PostgresContainerInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        final var container = new PostgreSQLContainer("postgres:16");
        container.start();
        log.info("container.getJdbcUrl():: {}", container.getJdbcUrl());
        log.info("container.getFirstMappedPort():: {}", container.getFirstMappedPort());

        configurableApplicationContext
                .addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> container.stop());

        configurableApplicationContext
                .getEnvironment()
                .getPropertySources()
                .addFirst(
                        new MapPropertySource("tc",
                                Map.of("datasource.url", container.getJdbcUrl(),
                                        "datasource.username", container.getUsername(),
                                        "datasource.password", container.getPassword()
                                )
                        )
                );
    }
}
