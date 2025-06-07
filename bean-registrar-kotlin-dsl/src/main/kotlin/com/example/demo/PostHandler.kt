package com.example.demo

import org.springframework.web.reactive.function.server.*
import java.net.URI
import java.util.*


class PostHandler(val posts: PostRepository) {

    suspend fun all(req: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyAndAwait(this.posts.findAll())
    }

    suspend fun create(req: ServerRequest): ServerResponse {
        val body = req.awaitBody(Post::class)
        val saved = this.posts.save(body)
        return ServerResponse.created(URI.create("/posts/$saved")).buildAndAwait()
    }

    suspend fun get(req: ServerRequest): ServerResponse {
        val id = UUID.fromString(req.pathVariable("id"))
        val found = this.posts.findById(id)
        return found?.let { ServerResponse.ok().bodyValueAndAwait(found) }
            ?: ServerResponse.notFound().buildAndAwait()
    }

    suspend fun update(req: ServerRequest): ServerResponse {
        val body = req.awaitBody(Post::class)
        val id = UUID.fromString(req.pathVariable("id"))
        return if (this.posts.update(id, body) > 0) {
            ServerResponse.noContent().buildAndAwait()
        } else {
            ServerResponse.notFound().buildAndAwait()
        }

    }

    suspend fun delete(req: ServerRequest): ServerResponse {
        val id = UUID.fromString(req.pathVariable("id"))
        return if (this.posts.deleteById(id) > 0) {
            ServerResponse.noContent().buildAndAwait()
        } else {
            ServerResponse.notFound().buildAndAwait()
        }

    }
}