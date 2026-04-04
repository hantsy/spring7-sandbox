package com.example.demo;


import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.Body;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;
import wiremock.com.fasterxml.jackson.annotation.JsonInclude;
import wiremock.com.fasterxml.jackson.databind.DeserializationFeature;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;
import wiremock.com.fasterxml.jackson.databind.SerializationFeature;
import wiremock.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@WireMockTest(httpPort = 9090)
public class WebTestClientTest {

    static {
        ObjectMapper wireMockObjectMapper = Json.getObjectMapper();
        wireMockObjectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        wireMockObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        wireMockObjectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        wireMockObjectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);

        JavaTimeModule module = new JavaTimeModule();
        wireMockObjectMapper.registerModule(module);
    }

    @TestConfiguration
    @Import(JacksonJsonMapperConfig.class)
    static class TestConfig {
    }

    @Autowired
    JsonMapper jsonMapper;

    WebTestClient client;

    @BeforeEach
    public void setup() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:9090")
                .codecs(c -> c.defaultCodecs()
                        .jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper))
                )
                .build();
    }

    @Test
    public void testGetAllPosts() {
        var data = List.of(
                new Post(UUID.randomUUID(), "title1", "content1", Status.DRAFT, LocalDateTime.now()),
                new Post(UUID.randomUUID(), "title2", "content2", Status.PUBLISHED, LocalDateTime.now())
        );
        stubFor(get("/posts")
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withResponseBody(Body.fromJsonBytes(Json.toByteArray(data)))
                )
        );

        client.get().uri("/posts").accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("size()").isEqualTo(2);

        verify(getRequestedFor(urlEqualTo("/posts"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    public void testGetPostById() {
        var id = UUID.randomUUID();
        var data = new Post(id, "title1", "content1", Status.DRAFT, LocalDateTime.now());

        stubFor(get("/posts/" + id)
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withResponseBody(Body.fromJsonBytes(Json.toByteArray(data)))
                )
        );

        client.get().uri("/posts/{id}", id).accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Post.class).value(post -> {
                            assertThat(post.id()).isEqualTo(id);
                            assertThat(post.title()).isEqualTo(data.title());
                            assertThat(post.content()).isEqualTo(data.content());
                            assertThat(post.status()).isEqualTo(data.status());
                            assertThat(post.createdAt()).isEqualTo(data.createdAt());
                        }
                );

        verify(getRequestedFor(urlEqualTo("/posts/" + id))
                .withHeader("Accept", equalTo("application/json"))
        );
    }

    @Test
    public void testCreatePost() {
        var id = UUID.randomUUID();
        var data = new Post(null, "title1", "content1", Status.DRAFT, null);

        stubFor(post("/posts")
                .willReturn(
                        aResponse()
                                .withHeader("Location", "/posts/" + id)
                                .withStatus(201)
                )
        );

        client.post().uri("/posts").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(data)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("/posts/" + id);

        verify(postRequestedFor(urlEqualTo("/posts"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(Json.write(data)))
        );
    }

    @Test
    public void testUpdatePost() {
        var id = UUID.randomUUID();
        var data = new Post(null, "title1", "content1", Status.DRAFT, null);

        stubFor(put("/posts/" + id)
                .willReturn(
                        aResponse()
                                .withStatus(204)
                )
        );

        client.put().uri("/posts/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(data)
                .exchange()
                .expectStatus().isNoContent();

        verify(putRequestedFor(urlEqualTo("/posts/" + id))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(Json.write(data)))
        );
    }

    @Test
    public void testDeletePostById() {
        var id = UUID.randomUUID();
        stubFor(delete("/posts/" + id)
                .willReturn(
                        aResponse()
                                .withStatus(204)
                )
        );

        client.delete().uri("/posts/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        verify(deleteRequestedFor(urlEqualTo("/posts/" + id)));
    }
}
