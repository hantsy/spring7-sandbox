package com.example.demo;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {

    private final CqlSession cqlSession;

    Product save(Product product) {
        var id = product.id() != null ? product.id() : UUID.randomUUID().toString();
        String query = """
                INSERT INTO products(id, name, price)
                VALUES (:id, :name, :price)
                """;
        ResultSet resultSet = cqlSession.execute(query, Map.of("id", id,
                "name", product.name(),
                "price", product.price())
        );
        log.debug("saving product: {}", resultSet.wasApplied());

        return new Product(id, product.name(), product.price());
    }

    Optional<Product> findById(String id) {
        String query = """
                SELECT * FROM products WHERE id = :id
                """;
        ResultSet resultSet = cqlSession.execute(query, Map.of("id", id));
        Row one = resultSet.one();
        if (one != null) {
            return Optional.of(
                    new Product(
                            one.get("id", String.class),
                            one.get("name", String.class),
                            one.get("price", BigDecimal.class)
                    )
            );
        }
        return Optional.empty();
    }
}
