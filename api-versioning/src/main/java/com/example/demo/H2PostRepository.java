package com.example.demo;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.BiFunction;

@Repository
public class H2PostRepository implements PostRepository {
    private static final Logger LOG = LoggerFactory.getLogger(H2PostRepository.class);

    private final DatabaseClient databaseClient;
    private static final BiFunction<Row, RowMetadata, Post> POST_MAPPER = (row, metadata) -> new Post(
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
                SELECT * FROM posts
                WHERE id = :id
                """;
        return databaseClient.sql(sql)
                .bind("id", id)
                .map(POST_MAPPER)
                .one()
                .onErrorResume(IncorrectResultSizeDataAccessException.class, e -> Mono.error(new PostNotFoundException(id)));
    }

    @Override
    public Flux<Post> findAll() {
        String sql = """
                SELECT * FROM posts
                """;
        return databaseClient.sql(sql)
                .map(POST_MAPPER)
                .all();
    }

    @Override
    public Mono<UUID> save(Post post) {
        String sql = """
                SELECT id FROM FINAL TABLE(
                    INSERT INTO posts(title, content)
                    VALUES (:title, :content)
                )
                """;
        return databaseClient.sql(sql)
                .bind("title", post.title())
                .bind("content", post.content())
                .fetch()
                .first()
                .map(row -> {
                    LOG.debug("Inserted post: {}", post);
                    return (UUID) row.get("id");
                });
    }

    @Override
    public Mono<Void> update(UUID id, Post post) {
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
                .rowsUpdated()

                .flatMap(updated -> {
                            if (updated != 1) {
                                return Mono.error(new PostNotFoundException(id));
                            } else {
                                return Mono.empty();
                            }
                        }
                );
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        String sql = """
                DELETE FROM posts
                WHERE id = :id
                """;
        return databaseClient.sql(sql)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .flatMap(updated -> {
                            if (updated != 1) {
                                return Mono.error(new PostNotFoundException(id));
                            } else {
                                return Mono.empty();
                            }
                        }
                );
    }
}
