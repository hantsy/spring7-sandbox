package com.example.demo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonObjectId;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {

    private final MongoClient  mongoClient;
    private MongoCollection<Product> productsCollection;

    @PostConstruct
    public void init() {
        this.productsCollection= mongoClient
                .getDatabase("test")
                .getCollection("products", Product.class);
    }

    Product save(Product product) {
        var result = this.productsCollection.insertOne(product);
        log.debug("save product result: {}", result);
        var id = result.getInsertedId().asObjectId().getValue().toHexString();
        return product.withId(id);
    }

    Optional<Product> findById(String id) {
       var byId = this.productsCollection
               .find(Filters.eq(new ObjectId(id)))
               .first();
       log.debug("find product by id: {}", byId);
       return Optional.ofNullable(byId);
    }

}
