package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@Component
public class PostClient {
    private final WebClient client;

    public PostClient(WebClient.Builder builder) {
        this.client = builder.build();
    }

    public Flux<Post> allPosts() {
        return client
                .get().uri("/posts")
                .exchangeToFlux(response -> response.bodyToFlux(Post.class));
    }

    public Mono<Post> getById(UUID id) {
        return client.get().uri("/posts/{id}", id)
                .retrieve()
                .onStatus(code -> code == HttpStatus.NOT_FOUND,
                        clientResponse -> {
                            throw new PostNotFoundException(id);
                        }
                )
                .bodyToMono(Post.class);
    }

    public Mono<URI> save(Post post) {
        return client.post().uri("/posts")
                .bodyValue(post)
                .retrieve()
                .toBodilessEntity()
                .map(entity -> entity.getHeaders().getLocation());
    }

    public Mono<Void> update(UUID id, Post post) {
        return client.put().uri("/posts/{id}", id)
                .bodyValue(post)
                .exchangeToMono(response -> response.bodyToMono(Void.class));
    }

    public Mono<Void> delete(UUID id) {
        return client.delete().uri("/posts/{id}", id)
                .exchangeToMono(response -> response.bodyToMono(Void.class));
    }
}
