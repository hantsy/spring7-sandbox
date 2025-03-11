package com.example.demo;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.BiFunction;

@Repository
public class H2PostRepository implements PostRepository {

    private final DatabaseClient databaseClient;
    private final BiFunction<Row, RowMetadata, Post> postMapper = (row, metadata) -> new Post(
        row.get("id", UUID.class),
        row.get("title", String.class),
        row.get("content", String.class)
    );

    public H2PostRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Post> findById(UUID id) {
        String sql = """
                SELECT * FROM posts WHERE id = :id
                """;
        return databaseClient.sql(sql)
                .bind("id", id)
                .map(postMapper)
                .one();
    }

    @Override
    public Flux<Post> findAll() {
        String sql = """
                SELECT * FROM posts
                """;
        return databaseClient.sql(sql)
                .map(postMapper)
                .all();
    }

    @Override
    public Mono<UUID> save(Post post) {
        String sql = """
                INSERT INTO posts (title, content)
                VALUES (:title, :content)
                RETURNING id
                """;
        return databaseClient.sql(sql)
                .bind("title", post.title())
                .bind("content", post.content())
                .fetch()
                .first()
                .map(stringObjectMap ->(UUID) stringObjectMap.get("id"));
    }

    @Override
    public Mono<Long> update(UUID id, Post post) {
        String sql = """
                UPDATE posts
                SET title = :title, content = :content 
                WHERE id = :id
                """;
        return databaseClient.sql(sql)
                .bind("title", post.title())
                .bind("content", post.content())
                .bind("id", id)
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Long> deleteById(UUID id) {
        String sql = """
                DELETE FROM posts
                WHERE id = :id
                """;
        return databaseClient.sql(sql)
                .bind("id", id)
                .fetch()
                .rowsUpdated();
    }
}
