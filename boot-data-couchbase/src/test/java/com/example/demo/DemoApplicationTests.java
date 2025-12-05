package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

	@Autowired
	private ProductRepository productRepository;

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
}
