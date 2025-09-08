package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Component
public class PostClient {
    private final static Logger log = LoggerFactory.getLogger(PostClient.class);
    private final RestClient client;

    public PostClient(RestClient.Builder builder) {
        this.client = builder.build();
    }

    List<Post> allPosts() {
        return client.get().uri("/posts")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    Post getById(UUID id) {
        var response = client.get().uri("/posts/{id}", id)
                .retrieve()
                .onStatus((HttpStatusCode s) -> s == HttpStatus.NOT_FOUND,
                        (HttpRequest req, ClientHttpResponse res) -> {
                            throw new PostNotFoundException(id);
                        }
                )
                .toEntity(Post.class);
        log.debug("response status code: {}", response.getStatusCode());
        return response.getBody();
    }

    URI save(Post post) {
        var response = client.post().uri("/posts")
                .body(post)
                .retrieve()
                .toBodilessEntity();

        URI location = response.getHeaders().getLocation();
        log.debug("saved location:" + location);
        return location;
    }

    void update(UUID id, Post post) {
        var response = client.put().uri("/posts/{id}", id)
                .body(post)
                .retrieve()
                .toBodilessEntity();

        log.debug("updated post status:" + response.getStatusCode());
    }

    void delete(UUID id) {
        var response = client.delete().uri("/posts/{id}", id)
                .retrieve()
                .toBodilessEntity();

        log.debug("deleted post status:" + response.getStatusCode());
    }

}
