package com.example.demo;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest
public class PostClientWithRestClientTest {
    private final static Logger log = LoggerFactory.getLogger(PostClientWithRestClientTest.class);

    @TestConfiguration
    @Import(PostClient.class)
    static class TestConfig{}

    @Autowired
    MockRestServiceServer server;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    PostClient client;

    @BeforeEach
    public void setup() {
        server.reset();
    }

    @Test
    public void testGetAllPosts() {
        var data = List.of(
                new Post(UUID.randomUUID(), "title1", "content1", Status.DRAFT, LocalDateTime.now()),
                new Post(UUID.randomUUID(), "title2", "content2", Status.PUBLISHED, LocalDateTime.now())
        );
        server.expect(ExpectedCount.once(), requestTo("/posts"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(jsonMapper.writeValueAsBytes(data), MediaType.APPLICATION_JSON)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                );

        var posts = client.allPosts();
        assertThat(posts.size()).isEqualTo(2);

        server.verify();
    }

    @Test
    public void testGetPostById() {
        var id = UUID.randomUUID();
        var data = new Post(id, "title1", "content1", Status.DRAFT, LocalDateTime.now());

        server.expect(ExpectedCount.once(), requestTo("/posts/" + id))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonMapper.writeValueAsBytes(data), MediaType.APPLICATION_JSON));

        var post = client.getById(id);
        assertThat(post.id()).isEqualTo(id);
        assertThat(post.title()).isEqualTo(data.title());
        assertThat(post.content()).isEqualTo(data.content());
        assertThat(post.status()).isEqualTo(data.status());
        assertThat(post.createdAt()).isEqualTo(data.createdAt());

        server.verify();
    }

    @Test
    public void testCreatePost() {
        var id = UUID.randomUUID();
        var data = new Post(null, "title1", "content1", Status.DRAFT, null);

        server.expect(ExpectedCount.once(), requestTo("/posts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().bytes(jsonMapper.writeValueAsBytes(data)))
                .andRespond(withStatus(HttpStatus.CREATED).location(URI.create("/posts/" + id)));

        var uri = client.save(data);
        //assertThat(uri).isEqualTo("/posts/" + id);
        log.debug("The location URI of the saved post:{}", uri);

        server.verify();
    }

    @Test
    public void testUpdatePost() {
        var id = UUID.randomUUID();
        var data = new Post(null, "title1", "content1", Status.DRAFT, null);

        server.expect(ExpectedCount.once(), requestTo("/posts/" + id))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.update(id, data);

        server.verify();
    }

    @Test
    public void testDeletePostById() {
        var id = UUID.randomUUID();

        server.expect(ExpectedCount.once(), requestTo("/posts/" + id))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));


        client.delete(id);

        server.verify();
    }
}
