package com.example.demo;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest;
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataElasticsearchTest(
        properties = {"spring.elasticsearch.username=elastic", "spring.elasticsearch.password=changeme"}
)
@Testcontainers
@Slf4j
public class Elasticsearch8IntegrationTests {
    private final static String IMAGE_NAME = "docker.elastic.co/elasticsearch/elasticsearch:9.2.2";

    @Container
    public static ElasticsearchContainer ES_CONTAINER = new ElasticsearchContainer(IMAGE_NAME);

    @DynamicPropertySource
    static void registerElasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "https://" + ES_CONTAINER.getHttpHostAddress());
    }

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testDatabaseIsRunning() {
        assertThat(ES_CONTAINER.isRunning()).isTrue();
    }

    @Test
    public void testProductRepository() {
        var product = productRepository.save(new Product(null, "test", BigDecimal.ONE));
        assertThat(product).isNotNull();
        assertThat(product.id()).isNotNull();

        Optional<Product> byId = productRepository.findById(product.id());
        assertThat(byId).isPresent();

        var p = byId.get();
        log.debug("found product by id: {}", p);
        assertThat(p.name()).isEqualTo("test");
    }

    @TestConfiguration
    static class SSL {

        @Bean
        public Rest5ClientBuilderCustomizer customizer() {
            return builder -> builder.setSSLContext(ES_CONTAINER.createSslContextFromCa());
        }
    }
}
