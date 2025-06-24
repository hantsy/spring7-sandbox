package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {R2dbcConfig.class, H2PostRepository.class})
class PostRepositoryTest {

    @Autowired
    PostRepository postRepository;

    @Test
    void testPostRepository() {
        assertThat(this.postRepository).isInstanceOf(H2PostRepository.class);
    }

    @Test
    void testCurdOperations() {
        Post post = new Post(null, "Test Title", "Test Content");
        Mono<UUID> savedPostId = this.postRepository.save(post);

        savedPostId.flatMap(id -> this.postRepository.findById(id))
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                    assertThat(p.title()).isEqualTo("Test Title");
                    assertThat(p.content()).isEqualTo("Test Content");
                })
                .verifyComplete();

        savedPostId.flatMap(id ->
                        this.postRepository.update(id, new Post(null, "Updated Title", "Updated Content"))
                )
                .doOnSuccess(v -> System.out.println(" Post is updated successfully"))
                .subscribe();

        this.postRepository.findAll().subscribe(System.out::println);

        savedPostId.flatMap(id -> this.postRepository.deleteById(id))
                .doOnSuccess((v) -> System.out.println("Post is deleted successfully"))
                .subscribe();

        this.postRepository.findAll().subscribe(System.out::println);
    }
}
