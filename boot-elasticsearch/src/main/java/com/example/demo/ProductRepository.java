package com.example.demo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {

    private final ElasticsearchClient client;

    @SneakyThrows
    Product save(Product product) {
        var id = product.id() != null ? product.id() : UUID.randomUUID().toString();
        log.debug("Saving product with id={}", id);
        IndexResponse response = client.index(builder -> builder.id(id).index("products").document(product));

        var savedID = response.id();
        log.debug("Saved product with id={}", savedID);
        return new Product(savedID, product.name(), product.price());
    }

    @SneakyThrows
    Optional<Product> findById(String id) {
        GetResponse<Product> response = this.client.get(builder -> builder.id(id).index("products"), Product.class);

        if (response.found()) {
            return Optional.ofNullable(response.source());
        }

        return Optional.empty();
    }
}
