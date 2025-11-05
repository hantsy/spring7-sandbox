package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;

@WebMvcTest(controllers = GreetingController.class)
public class GreetingControllerTest {

    @Autowired
    MockMvc mockMvc;

    private RestTestClient testClient;

    @BeforeEach
    public void setup() {
        this.testClient = RestTestClient.bindTo(mockMvc)
                .defaultApiVersion("1.0")
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();
    }

    @Test
    void testHello() {
        this.testClient.get().uri("/hello")
                // .apiVersion("1.0")
                .exchange()
                .expectBody(String.class).isEqualTo("Hello v1.0(Default)");
    }

    @Test
    void testHello1_1() {
        this.testClient.get().uri("/hello")
                .apiVersion("1.1")
                .exchange()
                .expectBody(String.class).isEqualTo("Hello v1.1");
    }

    @Test
    void testHello2_0() {
        this.testClient.get().uri("/hello")
                .apiVersion("2.0")
                .exchange()
                .expectBody(String.class).isEqualTo("Hello v2.0");
    }
}
