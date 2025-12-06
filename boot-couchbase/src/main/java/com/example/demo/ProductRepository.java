package com.example.demo;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {
    private final Cluster cluster;

    private Collection productCollection;

    @PostConstruct
    public void init() {
        this.productCollection = this.cluster
                .bucket("demo")
                .defaultCollection(); // or collections().createCollection() 
    }

    Product save(Product product) {
        String id = product.id() != null ? product.id() : UUID.randomUUID().toString();
        MutationResult result = this.productCollection.upsert(id, product);
        log.debug("saving product result: {}", result);

        return new Product(id, product.name(), product.price());
    }

    Optional<Product> findById(String id) {
        try {
            GetResult result = this.productCollection.get(id);
            return Optional.of(result.contentAs(Product.class));
        } catch (DocumentNotFoundException e) {
            return Optional.empty();
        }
    }
}
