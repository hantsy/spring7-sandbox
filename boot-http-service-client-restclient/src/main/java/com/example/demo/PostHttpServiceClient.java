package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.*;
import org.springframework.web.service.registry.HttpServiceClient;

import java.util.List;
import java.util.UUID;

@HttpExchange(url = "/posts", accept = "application/json", contentType = "application/json")
@HttpServiceClient("post")
public interface PostHttpServiceClient {
    @GetExchange("")
    List<Post> allPosts();

    @GetExchange("/{id}")
    Post getById(@PathVariable("id") UUID id);

    @PostExchange("")
    ResponseEntity<Void> save(@RequestBody Post post);

    @PutExchange("/{id}")
    ResponseEntity<Void> update(@PathVariable UUID id, @RequestBody Post post);

    @DeleteExchange("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
