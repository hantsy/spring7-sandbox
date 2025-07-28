package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = GreetingController.class)
public class GreetingControllerTest {

    WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        this.webTestClient = WebTestClient
                .bindToController(new GreetingController())
                .apiVersioning(apiVersionConfigurer ->
                        apiVersionConfigurer.useRequestHeader("X-API-Version"))
                .build();
    }

    @Test
    void testHello() {
        this.webTestClient.get().uri( "/hello")
                .apiVersion(1.0)
                .exchange()
                .expectBody(String.class).isEqualTo("Hello v1.0(Default)");
    }

    @Test
    void testHello1_1() {
        this.webTestClient.get().uri("/hello")
                .apiVersion(1.1)
                .exchange()
                .expectBody(String.class).isEqualTo("Hello v1.1");
    }

    @Test
    void testHello2_0() {
        this.webTestClient.get().uri("/hello")
                .apiVersion(2.0)
                .exchange()
                .expectBody(String.class).isEqualTo("Hello v2.0");
    }
}
