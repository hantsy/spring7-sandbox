package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@SpringJUnitConfig(classes = {CustomConfig.class, R2dbcConfig.class})
@ActiveProfiles("h2")
class H2ApplicationTest {

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
                .subscribe(updatedCount -> System.out.println("Updated rows: " + updatedCount));

        this.postRepository.findAll().subscribe(System.out::println);

        savedPostId.flatMap(id -> this.postRepository.deleteById(id))
                .subscribe(System.out::println);

        this.postRepository.findAll().subscribe(System.out::println);
    }
}
