package com.example.demo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {
    private final Driver driver;

    private Session session;

    @PostConstruct
    public void init() {
        this.session = driver.session();
    }

    Product save(Product product) {
        String query = """
                MERGE (p:Product {id: $id})
                ON CREATE SET p.name=$name, p.price=$price
                ON MATCH SET p.name=$name, p.price=$price
                RETURN p.id as id, p.name as name, p.price as price
                """;
        var result = this.session
                .executeWrite(tc -> tc.run(query,
                                        Map.of("id", product.id() != null ? product.id() : UUID.randomUUID().toString(),
                                                "name", product.name(),
                                                "price", Values.value(product.price().toString())
                                        )
                                )
                                .single()
                );

        log.debug("saving product {}", result);
        return new Product(result.get("id").asString(), result.get("name").asString(), new BigDecimal(result.get("price").asString()));
    }

    Optional<Product> findById(String id) {
        String query = """
                MATCH (p:Product)
                WHERE p.id=$id
                RETURN p.id as id, p.name as name, p.price as price
                """;
        var optionalRecord = this.session
                .executeRead(tc -> tc.run(query, Map.of("id", id))
                        .stream()
                        .findFirst()
                );
        log.debug("find product by id {}", optionalRecord);
        return optionalRecord
                .map(result -> new Product(result.get("id").asString(), result.get("name").asString(), new BigDecimal(result.get("price").asString())));

    }
}
