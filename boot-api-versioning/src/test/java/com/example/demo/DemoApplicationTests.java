package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {
    // This is a 4.0 break change.
    // The `LocalServerPort` is moved from package `org.springframework.boot.test.web.server`
    // to `org.springframework.boot.web.server.test`.
    // move back to `org.springframework.boot.test.web.server` in RC1
    @LocalServerPort
    int port;

    RestClient client;

    @BeforeEach
    public void setup() {
        var jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(10_000))
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(jdkHttpClient);

        this.client = RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .defaultApiVersion("1.0")
                .apiVersionInserter(ApiVersionInserter.builder()
                        .useHeader("X-API-Version")
                        // .usePathSegment(0)
                        // .useQueryParam("version")
                        // .withVersionFormatter(ApiVersionFormatter)
                        .build()
                )
                .requestFactory(requestFactory)
                .build();
    }

    @Test
    void testHello() {
        var hello = this.client.get().uri("/hello")
                .apiVersion(1.0)
                .retrieve()
                .body(String.class);

        assertThat(hello).isEqualTo("Hello v1.0(Default)");
    }

    @Test
    void testHello1_1() {
        var hello = this.client.get().uri("/hello")
                .apiVersion(1.1)
                .retrieve()
                .body(String.class);

        assertThat(hello).isEqualTo("Hello v1.1");
    }

    @Test
    void testHello2_0() {
        var hello = this.client.get().uri("/hello")
                .apiVersion(2.0)
                .retrieve()
                .body(String.class);

        assertThat(hello).isEqualTo("Hello v2.0");
    }

}
