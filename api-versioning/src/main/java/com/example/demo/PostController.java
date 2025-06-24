/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.*;

/**
 * @author hantsy
 */
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostRepository posts;

    public PostController(PostRepository posts) {
        this.posts = posts;
    }

    @GetMapping
    public ResponseEntity<?> all() {
        return ok(this.posts.findAll());
    }

    @PostMapping
    public Mono<ResponseEntity<?>> create(@RequestBody Post post) {
        return this.posts.save(post).map(saved -> URI.create("/posts/" + saved))
                .map(uri -> created(uri).build());
    }

    @GetMapping("{id}")
    public Mono<ResponseEntity<?>> getById(@PathVariable UUID id) {
        return this.posts.findById(id)
                .map(ResponseEntity::ok);
    }

    @PutMapping("{id}")
    public Mono<ResponseEntity<?>> update(@PathVariable UUID id, @RequestBody Post post) {
        return this.posts.findById(id)
                .map(existed -> new Post(existed.id(), post.title(), post.content()))
                .flatMap(updated -> this.posts.update(id, updated))
                .then(Mono.fromCallable(() -> noContent().build()));
    }

    @DeleteMapping("{id}")
    public Mono<ResponseEntity<?>> delete(@PathVariable UUID id) {
        return this.posts.deleteById(id)
                .then(Mono.fromCallable(() -> noContent().build()));
    }
}
