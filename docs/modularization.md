#  Spring Boot 4 Modularization

One breaking change in Spring Boot 4 is that the single monolithic `spring-boot-autoconfigure` module is split into smaller, fine-grained feature and library-specific modules. Each feature or library now provides its own `starter` and `starter-test` modules, which allow developers to include only the autoconfiguration classes and third-party libraries their application requires.

## Transforming to Spring Boot 4

Visit [Spring Initializr](https://start.spring.io/) and generate both a Spring Boot 3 and a Spring Boot 4 project using the same dependencies: `Web`, `Data JPA`, and `Security`. Then compare the generated `pom.xml` files to observe how the dependency structure differs.

Spring Boot 3 employs a single unified test starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
</dependency>
```

In contrast, Spring Boot 4 demonstrates that `spring-boot-starter-web` has been renamed to `spring-boot-starter-webmvc`, and each feature module provides dedicated test starters.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Spring Boot 4 distributes autoconfiguration classes—previously consolidated in the monolithic `spring-boot-autoconfigure` module—across feature-specific modules instead. For instance, `spring-boot-starter-security-test` includes `spring-security-test` dependency along with its corresponding autoconfiguration classes. Additionally, the package hierarchy has been reorganized around features; for example, `...autoconfigure.data.jpa` is now `...data.jpa.autoconfigure`.

For a complete list of Spring Boot 4 modules, see the [Module dependencies section of the Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#module-dependencies).

For applications requiring only specific lightweight features—such as Jackson, Flyway, or Liquibase—dedicated `starter` and `starter-test` modules are now available. Here's how to add Jackson:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jackson</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jackson-test</artifactId>
    <scope>test</scope>
</dependency>
```

We have discussed the [new Jackson 3 support in Spring 7 and Spring Boot 4](./jackson.md).

In Spring Boot 3, using `RestClient` or `WebClient` out-of-box required adding `spring-boot-starter-web` or `spring-boot-starter-webflux`, which introduced a sizable collection of dependencies and autoconfiguration overhead. Spring Boot 4 streamlines this by offering the lightweight `spring-boot-starter-restclient` and `spring-boot-starter-webclient` modules, which include only the necessary client libraries and their associated configurations.

Now, let's examine Spring Boot 4's enhanced `RestClient` and `WebClient` capabilities more thoroughly.

## Spring Web Client Modules

Leverage the existing `Post` model and assume a RESTful API with the following core operations: 

* `GET /posts` — retrieves all posts
* `GET /posts/{id}` — retrieves a post by ID; returns a 404 (NOT FOUND) status if it doesn't exist
* `POST /posts` — accepts a `Post` entity in the request body and returns a 201 (CREATED) status with a `Location` header pointing to the newly created resource
* `PUT /posts/{id}` — updates the specified post by ID and returns a 204 (NO CONTENT) status
* `DELETE /posts/{id}` — removes the specified post by ID and returns a 204 (NO CONTENT) status

We'll begin by exploring `RestClient` support in Spring Boot 4, which builds upon the `RestClient` API introduced in Spring Framework 6.0. 

### RestClient

Create a new Spring Boot 4 project through [Spring Initializr](https://start.spring.io/) with the `Http Client` dependency, then manually include `spring-boot-starter-jackson`. The `spring-boot-starter-restclient` starter automatically configures a `RestClient.Builder`, while `spring-boot-starter-restclient-test` provides test utilities including `RestClient/RestTemplate` mocks and `MockRestServiceServer` for verification.

Now create a `PostClient` class that leverages `RestClient` to implement these operations:

```java
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
```

Create a test class to verify functionality, and use `MockRestServiceServer` to mock the server and provide RESTful APIs.

```java
@RestClientTest
public class PostClientWithMockRestServiceServerTest {
    private final static Logger log = LoggerFactory.getLogger(PostClientWithMockRestServiceServerTest.class);

    @TestConfiguration
    @Import(PostClient.class)
    static class TestConfig {
    }

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
    public void testGetPostById_NotFound() {
        var id = UUID.randomUUID();
        server.expect(ExpectedCount.once(), requestTo("/posts/" + id))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getById(id)).isInstanceOf(PostNotFoundException.class);

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
```

The `@RestClientTest` annotation provides a `MockRestServiceServer` instance and configures a minimal test environment tailored for REST client verification.

To customize `RestClient.Builder` behavior, implement a `RestClientCustomizer` bean: 

```java
@Bean
RestClientCustomizer restClientCustomizer(JsonMapper mapper) {
    return builder -> builder
            .baseUrl("http://localhost:9090")
            .configureMessageConverters(c -> c.registerDefaults()
                    .withJsonConverter(new JacksonJsonHttpMessageConverter(mapper))
            )
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
}
```

You can further customize the underlying HTTP client engine by configuring a `ClientHttpRequestFactoryBuilder` and `ClientHttpRequestFactoryBuilderCustomizer`. Here's an example using the JDK HTTP client:

```java
@Bean
ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder() {
    return ClientHttpRequestFactoryBuilder.jdk();
//                .withCustomizer()
//                .withHttpClientCustomizer()
//                .withExecutor()
}
```

#### RestTestClient

`RestTestClient`, located in the `spring-test` module, adopts the `RestClient` design pattern and provides additional test-specific methods for convenient assertions and verification.

In parallel with the existing reactive `WebTestClient`, `RestTestClient` offers analogous APIs for synchronous (blocking) scenarios. You can instantiate `RestTestClient` targeting a controller class, `RouterFunction`, `ApplicationContext`, or a remote server. The following demonstrates connecting to a remote server and verifying REST endpoints:

```java
@SpringBootTest
@WireMockTest(httpPort = 9090)
public class RestTestClientTest {
    private final static Logger log = LoggerFactory.getLogger(RestTestClientTest.class);

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
    class TestConfig {
    }

    @Autowired
    JsonMapper jsonMapper;

    RestTestClient client;

    @BeforeEach
    public void setup() {
        client = RestTestClient.bindToServer()
                .configureMessageConverters(c -> c.registerDefaults()
                        .withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper))
                )
                .baseUrl("http://localhost:9090")
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
                .expectBody(Post.class)
                .value(p -> {
                    Assertions.assertThat(p.id()).isEqualTo(id);
                    Assertions.assertThat(p.title()).isEqualTo(data.title());
                    Assertions.assertThat(p.content()).isEqualTo(data.content());
                    Assertions.assertThat(p.status()).isEqualTo(data.status());
                    Assertions.assertThat(p.createdAt()).isEqualTo(data.createdAt());
                });

        verify(getRequestedFor(urlEqualTo("/posts/" + id))
                .withHeader("Accept", equalTo("application/json"))
        );
    }

    @Test
    public void testGetPostById_NotFound() {
        var id = UUID.randomUUID();

        stubFor(get("/posts/" + id)
                .willReturn(aResponse().withStatus(404))
        );

        client.get().uri("/posts/{id}", id).accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

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
                                .withResponseBody(Body.none())
                )
        );

        client
                .post().uri("/posts").contentType(MediaType.APPLICATION_JSON).body(data)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("/posts/" + id);

        verify(postRequestedFor(urlEqualTo("/posts"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(jsonMapper.writeValueAsString(data)))
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

        client.put().uri("/posts/{id}", id).contentType(MediaType.APPLICATION_JSON).body(data)
                .exchange()
                .expectStatus().isNoContent();

        verify(putRequestedFor(urlEqualTo("/posts/" + id))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(jsonMapper.writeValueAsString(data)))
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
```

This example uses `WireMock` to simulate a remote server and stub REST endpoints.

For the complete working example, visit the [GitHub repository](https://github.com/hantsy/spring7-sandbox/tree/master/boot-restclient), which also includes comprehensive tests demonstrating `RestClient` validation against `WireMock`.  

### WebClient

Previous articles have explored the reactive `WebClient` and test-focused `WebTestClient` comprehensively. Here, we'll provide a concise overview of their capabilities.

Begin by creating a Spring Boot 4 project from [Spring Initializr](https://start.spring.io) with the `Reactive HTTP Client` dependency. Additionally, add `spring-boot-starter-jackson` for Jackson 3 support. The `spring-boot-starter-webclient` starter automatically configures a `WebClient.Builder`, while `spring-boot-starter-webclient-test` furnishes test-specific `WebClient.Builder` configurations.

Implement a `PostClient` class that leverages `WebClient`:

```java
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
```

Now create a test class to validate the functionality using `WireMock` to simulate the remote server and stub REST endpoints:

```java
@SpringBootTest
@WireMockTest(httpPort = 9090)
public class PostClientTest {

    static {
        ObjectMapper wireMockObjectMapper = Json.getObjectMapper();
        wireMockObjectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        wireMockObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        wireMockObjectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        wireMockObjectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);

        JavaTimeModule module = new JavaTimeModule();
        wireMockObjectMapper.registerModule(module);
    }

    @Autowired
    PostClient client;

    @BeforeEach
    public void setup() {
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

        client.allPosts()
                .as(StepVerifier::create)
                .expectNextCount(2)
                .verifyComplete();

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

        client.getById(id)
                .as(StepVerifier::create)
                .consumeNextWith(
                        post -> {
                            assertThat(post.id()).isEqualTo(id);
                            assertThat(post.title()).isEqualTo(data.title());
                            assertThat(post.content()).isEqualTo(data.content());
                            assertThat(post.status()).isEqualTo(data.status());
                            assertThat(post.createdAt()).isEqualTo(data.createdAt());
                        }
                )
                .verifyComplete();

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

        client.save(data)
                .as(StepVerifier::create)
                .consumeNextWith(
                        uri -> {
                            assertThat(uri.toString()).isEqualTo("/posts/" + id);
                        }
                )
                .verifyComplete();

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

        client.update(id, data)
                .as(StepVerifier::create)
                .thenAwait()
                .verifyComplete();

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

        client.delete(id)
                .as(StepVerifier::create)
                .thenAwait()
                .verifyComplete();

        verify(deleteRequestedFor(urlEqualTo("/posts/" + id)));
    }
}
```

#### WebTestClient

`WebTestClient` is a test client designed to work seamlessly with a controller class, `RouterFunction`, `ApplicationContext`, or a remote server. The example below demonstrates connecting to a remote server while employing `WireMock` to stub REST endpoints. 

```java
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
```

For the complete working example, visit the [GitHub repository](https://github.com/hantsy/spring7-sandbox/tree/master/boot-webclient).

Spring Boot 4 also modularizes Spring Data dependencies by moving database drivers and client SDKs into dedicated modules. For example:

- JDBC `JdbcClient` and R2DBC `DatabaseClient` are now exposed by `spring-boot-starter-jdbc` and `spring-boot-starter-r2dbc`.
- MongoDB Java client support (synchronous or reactive `MongoClient`) is exposed by `spring-boot-starter-mongodb`.

This makes it possible to depend on the specific client libraries you need without pulling in full Spring Data starter sets.

Now let's examine Spring Boot 4's revamped Spring Data client support across multiple persistence layers.

## Spring Data Client Modules

We'll begin with JDBC `JdbcClient` and R2DBC `DatabaseClient` support, building on earlier explorations of these technologies.

### JDBC `JdbcClient`

Begin by generating a Spring Boot 4 project from [Spring Initializr](https://start.spring.io/) with `JDBC API`, `PostgreSQL`, `Lombok`, and `Testcontainers` dependencies. The `spring-boot-starter-jdbc` starter automatically configures `JdbcTemplate`, `NamedParameterJdbcTemplate`, and `JdbcClient`.

Create a simple record type `Post` to represent the row data in the database. 

```java
public record Post(UUID id,
                   String title,
                   String content,
                   Status status,
                   LocalDateTime createdAt
) {
    public static Post of(String title, String content) {
        return new Post(null, title, content, Status.DRAFT, null);
    }

    public static Post of(String title, String content, Status status) {
        return new Post(null, title, content, status, null);
    }
}

public enum Status {
    DRAFT, PENDING_MODERATION, PUBLISHED;
}
```

Define a `PostRepository` interface to declare the data access methods:

```java
public interface PostRepository {
    Post save(Post post);
    Post findById(UUID id);
    List<Post> findAll();
    Integer update(Post post);
    Integer deleteById(UUID id);
    Integer deleteAll();
    // other methods are omitted for brevity
}
```

Then implement the `PostRepository` interface using `JdbcClient`.

```java
@RequiredArgsConstructor
@Slf4j
@Repository
@Transactional
public class JdbcClientPostRepository implements PostRepository {

    public static final RowMapper<Post> ROW_MAPPER = (rs, rowNum) -> new Post(
            rs.getObject("id", UUID.class),
            rs.getString("title"),
            rs.getString("content"),
            //see: https://github.com/pgjdbc/pgjdbc/issues/2387
            //rs.getObject("status", Status.class),
            Status.valueOf(rs.getString("status")),
            rs.getObject("created_at", LocalDateTime.class)
    );

    private final JdbcClient client;


    @Override
    public List<Post> findAll() {
        var sql = "SELECT * FROM posts";
        return this.client.sql(sql)
                // added in Spring 7.0
                .withFetchSize(10)
                .withMaxRows(50)
                .withQueryTimeout(1_000)
                .query(ROW_MAPPER).list();
    }

    @Override
    public Post findById(UUID id) {
        var sql = "SELECT * FROM posts WHERE id=:id";
        return this.client.sql(sql).params(Map.of("id", id)).query(ROW_MAPPER).single();
    }

    @Override
    public UUID save(Post p) {
        var sql = """
                INSERT INTO  posts (title, content, status) 
                VALUES (:title, :content, :status)
                RETURNING id
                """;
        var keyHolder = new GeneratedKeyHolder();
        var paramSource = new MapSqlParameterSource(
                Map.of("title", p.title(), "content", p.content(), "status", p.status().name())
        );
        var cnt = this.client.sql(sql).paramSource(paramSource).update(keyHolder);
        log.debug("updated count:{}", cnt);
        return keyHolder.getKeyAs(UUID.class);
    }

    @Override
    public Integer update(Post p) {
        var sql = "UPDATE posts set title=:title, content=:content, status=:status WHERE id=:id";
        Map<String, ? extends Serializable> params = Map.of(
                "title", p.title(),
                "content", p.content(),
                "status", p.status().name(),
                "id", p.id()

        );
        return this.client.sql(sql).params(params).update();
    }

    @Override
    public Integer deleteById(UUID id) {
        var sql = "DELETE FROM posts WHERE id=:id";
        return this.client.sql(sql).params(Map.of("id", id)).update();
    }

}
```

Add a `schema.sql` file and `data.sql` in `src/main/resources` to initialize the database schema and data.

```sql
-- schema.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- create posts table
CREATE TABLE IF NOT EXISTS posts (
 id UUID DEFAULT uuid_generate_v4(),
 title VARCHAR(255),
 content VARCHAR(255),
 status VARCHAR(20) DEFAULT 'DRAFT',
 created_at TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP,
 PRIMARY KEY (id)
 );

-- data.sql
INSERT INTO  posts (title, content) VALUES ('Spring 6 and Jdbc', 'Review the Jdbc features in Spring framework 6.0');
```

Create a test class to verify the `PostRepository` implementation against a real PostgreSQL database running in Testcontainers.

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    @Autowired
    PostRepository posts;

    @BeforeEach
    public void setup() {
        var deleted = this.posts.deleteAll();
        log.debug("deleted posts: {}", deleted);
    }

    @Test
    public void testSaveAll() {
        var data = List.of(
                Post.of("test", "content", Status.PENDING_MODERATION),
                Post.of("test2", "content2"),
                Post.of("test3", "content3")
        );

        data.forEach(post -> {
            var saved = posts.save(post);
            log.debug("saved post: {}", saved);
        });

        Long count = posts.count();
        assertThat(count).isEqualTo(3L);

        var countByStatus = posts.countByStatus();
        log.debug("count by status: {}", countByStatus);
        assertThat(countByStatus.get(Status.DRAFT)).isEqualTo(2L);
    }

    @Test
    public void testInsertAndQuery() {
        var id = this.posts.save(Post.of("test title", "test content"));
        var saved = this.posts.findById(id);
        assertThat(saved.status()).isEqualTo(Status.DRAFT);

        var updatedCnt = this.posts.update(new Post(saved.id(), "updated test", "updated content", Status.PENDING_MODERATION, saved.createdAt()));
        assertThat(updatedCnt).isEqualTo(1);
        var updated = this.posts.findById(id);
        assertThat(updated.status()).isEqualTo(Status.PENDING_MODERATION);
    }

}
```

The complete implementation includes a `DataInitializer` component that uses `ApplicationReadyEvent` observation to populate initial data. Review the [GitHub repository](https://github.com/hantsy/spring7-sandbox/tree/master/boot-jdbc) for details.

### R2DBC `DatabaseClient`

Similarly, scaffold a Spring Boot 4 project using `R2DBC API`, `PostgreSQL`, `Lombok`, and `Testcontainers` dependencies. The `spring-boot-starter-r2dbc` starter provides automatic configuration of `DatabaseClient`. Since Spring leverages Project Reactor for reactive streams, repository methods return `Mono` or `Flux` types rather than direct values.

The `Post` record and `Status` enum are the same as the JDBC example above, so we can reuse them here.

The `PostRepository` interface is also similar, but the return types are now reactive types.

```java
public interface PostRepository {
    Flux<Post> findByTitleContains(String name);
    Mono<Post> save(Post post);
    Mono<Post> findById(UUID id);
    Flux<Post> findAll();
    Mono<Integer> update(Post post);
    Mono<Integer> deleteById(UUID id);
    Mono<Integer> deleteAll();
    // other methods are omitted for brevity
}
```

Then implement the `PostRepository` interface using `DatabaseClient`.

```java
@RequiredArgsConstructor
@Component
@Slf4j
public class R2dbcClientPostRepository implements PostRepository {

    public static final BiFunction<Row, RowMetadata, Post> MAPPING_FUNCTION = (row, rowMetaData) -> new Post(
            row.get("id", UUID.class),
            row.get("title", String.class),
            row.get("content", String.class),
            Status.valueOf(row.get("status", String.class)),
            row.get("created_at", LocalDateTime.class)
    );

    private final DatabaseClient databaseClient;

    @Override
    public Flux<Post> findByTitleContains(String name) {
        String sql = """
                SELECT * FROM posts 
                         WHERE title LIKE :title
                """;
        return this.databaseClient
                .sql(sql)
                .bind("title", "%" + name + "%")
                .map(MAPPING_FUNCTION)
                .all();
    }

    @Override
    public Flux<Post> findAll() {
        String sql = """
                SELECT * FROM posts
                """;
        return this.databaseClient
                .sql(sql)
                .filter((statement, executeFunction) -> statement.fetchSize(10).execute())
                .map(MAPPING_FUNCTION)
                .all();
    }

    @Override
    public Mono<Post> findById(UUID id) {
        String sql = """
                SELECT * FROM posts 
                         WHERE id=:id
                """;
        return this.databaseClient
                .sql(sql)
                .bind("id", id)
                .map(MAPPING_FUNCTION)
                .one();
    }

    @Override
    public Mono<UUID> save(Post p) {
        String sql = """
                INSERT INTO  posts (title, content, status) 
                VALUES (:title, :content, :status)
                """;
        return this.databaseClient.sql(sql)
                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .bind("title", p.title())
                .bind("content", p.content())
                .bind("status", p.status().name())
                .fetch()
                .first()
                .map(r -> (UUID) r.get("id"));
    }

    // see: https://github.com/spring-projects/spring-data-r2dbc/issues/259
    // and
    // https://stackoverflow.com/questions/62514094/how-to-execute-multiple-inserts-in-batch-in-r2dbc
    @Override
    public Flux<UUID> saveAll(List<Post> data) {
        Assert.notEmpty(data, "saving data can be empty");
        return this.databaseClient.inConnectionMany(connection -> {

            String sql = """
                    INSERT INTO posts (title, content, status) 
                    VALUES ($1, $2, $3)
                    """;
            var statement = connection
                    .createStatement(sql)
                    .returnGeneratedValues("id");

            for (int i = 0; i < data.size() - 1; i++) {
                Post p = data.get(i);
                statement.bind(0, p.title())
                        .bind(1, p.content())
                        .bind(2, p.status().name())
                        .add();
            }

            // for the last item, do not call `add`
            var lastItem = data.get(data.size() - 1);
            statement.bind(0, lastItem.title())
                    .bind(1, lastItem.content())
                    .bind(2, lastItem.status().name());

            return Flux.from(statement.execute())
                    .flatMap(result -> result.map((row, rowMetadata) -> row.get("id", UUID.class)));
        });
    }

    @Override
    public Mono<Long> update(Post p) {
        String sql = """
                UPDATE posts 
                SET title=:title,
                    content=:content,
                    status=:status 
                WHERE id=:id
                """;
        return this.databaseClient
                .sql(sql)
                .bind("title", p.title())
                .bind("content", p.content())
                .bind("status", p.status().name())
                .bind("id", p.id())
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Long> deleteById(UUID id) {
        String sql = """
                DELETE FROM posts 
                       WHERE id=:id
                """;
        return this.databaseClient.sql(sql)
                .bind("id", id)
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Long> deleteAllById(List<UUID> ids) {
        String sql = """
                DELETE FROM posts 
                       WHERE id in (:ids)
                """;
        return this.databaseClient.sql(sql)
                .bind("ids", ids)
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Long> deleteAll() {
        String sql = """
                DELETE FROM posts
                """;
        return this.databaseClient.sql(sql)
                .fetch()
                .rowsUpdated();
    }
}
```

The test code parallels the JDBC example above; for brevity, we omit it here. For the full implementation, see the [GitHub repository](https://github.com/hantsy/spring7-sandbox/tree/master/boot-r2dbc). Note that the PostgreSQL Testcontainer requires the JDBC driver, necessitating both PostgreSQL R2DBC and JDBC dependencies in `pom.xml`.

### MongoDB `MongoClient`

Generate a Spring Boot 4 project with `MongoDB`, `Lombok`, and `Testcontainers` dependencies. Manually add the `org.testcontainers:testcontainers-mongodb` dependency to `pom.xml`. The `spring-boot-starter-mongodb` starter automatically configures a `MongoClient`.

Define a simple POJO class `Product` to represent MongoDB document data:

```java
@BsonDiscriminator("products")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Product {
    @BsonId @BsonRepresentation(BsonType.OBJECT_ID)
    String id;
    String name;
    BigDecimal price;

    Product withId(String newId) {
        return new Product(newId, this.name, this.price);
    }
}
```

> [!WARNING]
> At the time of writing, the MongoClient API does not support record types.

Implement a `ProductRepository` class that uses `MongoClient` to manipulate documents:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {

    private final MongoClient mongoClient;
    private MongoCollection<Product> productsCollection;

    @PostConstruct
    public void init() {
        this.productsCollection = mongoClient
                .getDatabase("test")
                .getCollection("products", Product.class);
    }

    Product save(Product product) {
        var result = this.productsCollection.insertOne(product);
        log.debug("save product result: {}", result);
        var id = result.getInsertedId().asObjectId().getValue().toHexString();
        return product.withId(id);
    }

    Optional<Product> findById(String id) {
        var byId = this.productsCollection
                .find(Filters.eq(new ObjectId(id)))
                .first();
        log.debug("find product by id: {}", byId);
        return Optional.ofNullable(byId);
    }

}
```

Create a test class to validate the `ProductRepository` implementation using a MongoDB instance from Testcontainers:

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void testProductRepository() {
        var product = productRepository.save(new Product(null, "test", BigDecimal.ONE));
        assertThat(product).isNotNull();
        assertThat(product.getId()).isNotNull();

        Optional<Product> byId = productRepository.findById(product.getId());
        assertThat(byId).isPresent();

        var p = byId.get();
        log.debug("found product by id: {}", p);
        assertThat(p.getName()).isEqualTo("test");
    }
}
```

Visit the [GitHub repository](https://github.com/hantsy/spring7-sandbox/tree/master/boot-mongodb) for the complete implementation. Note that Spring Initializr doesn't generate `TestcontainersConfiguration` for MongoDB automatically; you'll need to create it manually or copy from a project generated with `Spring Data MongoDB` included. 

### Neo4j `Driver`

Start with a Spring Boot 4 project configured with `Neo4j`, `Lombok`, and `Testcontainers` dependencies. Add `org.testcontainers:testcontainers-neo4j` to `pom.xml`. The `spring-boot-starter-neo4j` starter automatically configures a Neo4j `Driver`.

Define a record type `Product` to represent node data in Neo4j:

```java
public record Product(String id, String name, BigDecimal price) {
}
```

Implement a `ProductRepository` class that uses the `Driver` to interact with nodes:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {
    private final Driver driver;

    private Session session;

    @PostConstruct
    public void init() {
        this.session = driver.session();
    }

    Product save(Product product) {
        String query = """
                MERGE (p:Product {id: $id})
                ON CREATE SET p.name=$name, p.price=$price
                ON MATCH SET p.name=$name, p.price=$price
                RETURN p.id as id, p.name as name, p.price as price
                """;
        var result = this.session
                .executeWrite(tc -> tc.run(query,
                                        Map.of("id", product.id() != null ? product.id() : UUID.randomUUID().toString(),
                                                "name", product.name(),
                                                "price", Values.value(product.price().toString())
                                        )
                                )
                                .single()
                );

        log.debug("saving product {}", result);
        return new Product(result.get("id").asString(), result.get("name").asString(), new BigDecimal(result.get("price").asString()));
    }

    Optional<Product> findById(String id) {
        String query = """
                MATCH (p:Product)
                WHERE p.id=$id
                RETURN p.id as id, p.name as name, p.price as price
                """;
        var optionalRecord = this.session
                .executeRead(tc -> tc.run(query, Map.of("id", id))
                        .stream()
                        .findFirst()
                );
        log.debug("find product by id {}", optionalRecord);
        return optionalRecord
                .map(result -> new Product(result.get("id").asString(), result.get("name").asString(), new BigDecimal(result.get("price").asString())));

    }
}
```

Add a test class to verify the `ProductRepository` implementation against a real Neo4j database running in Testcontainers.

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

 @Autowired
 private ProductRepository productRepository;

    @Test
    public void testProductRepository() {
        var product = productRepository.save(new Product(null, "test", BigDecimal.ONE));
        assertThat(product).isNotNull();
        assertThat(product.id()).isNotNull();

        Optional<Product> byId = productRepository.findById(product.id());
        assertThat(byId).isPresent();
        var p = byId.get();

        log.debug("found product by id: {}", p);
        assertThat(p.name()).isEqualTo("test");
    }
}
```

View the [GitHub repository](https://github.com/hantsy/spring7-sandbox/tree/master/boot-neo4j) for the complete example. As with MongoDB, Spring Initializr doesn't auto-generate `TestcontainersConfiguration` for Neo4j, requiring manual creation or copying from a `Spring Data Neo4j` configuration.

### Elasticsearch `ElasticsearchClient`

Create a Spring Boot 4 project featuring `Elasticsearch`, `Lombok`, and `Testcontainers` dependencies. Add `org.testcontainers:testcontainers-elasticsearch` to `pom.xml`. The `spring-boot-starter-elasticsearch` starter automatically configures both `ElasticsearchClient` and a low-level REST client. We'll use the high-level `ElasticsearchClient` for document operations.

Define a record type `Product` to represent document data in Elasticsearch:

```java
public record Product(String id, String name, BigDecimal price) {
}
```

Implement a `ProductRepository` class that uses `ElasticsearchClient` to manage documents:

```java

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {

    private final ElasticsearchClient client;

    @SneakyThrows
    Product save(Product product) {
        var id = product.id() != null ? product.id() : UUID.randomUUID().toString();
        log.debug("Saving product with id={}", id);
        IndexResponse response = client.index(builder -> builder.id(id).index("products").document(product));

        var savedID = response.id();
        log.debug("Saved product with id={}", savedID);
        return new Product(savedID, product.name(), product.price());
    }

    @SneakyThrows
    Optional<Product> findById(String id) {
        GetResponse<Product> response = this.client.get(builder -> builder.id(id).index("products"), Product.class);

        if (response.found()) {
            return Optional.ofNullable(response.source());
        }

        return Optional.empty();
    }
}
```

Write a test class to verify the functionality: 

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

	@Autowired
	ProductRepository productRepository;

	@Test
	public void testProductRepository() {
		var product = productRepository.save(new Product(null, "test", BigDecimal.ONE));
		assertThat(product).isNotNull();
		assertThat(product.id()).isNotNull();

		Optional<Product> byId = productRepository.findById(product.id());
		assertThat(byId).isPresent();
		var p = byId.get();
		log.debug("found product by id: {}", p);
		assertThat(p.name()).isEqualTo("test");
	}

}
```

As before, manually create `TestcontainersConfiguration` or copy it from a project generated with `Spring Data Elasticsearch`. See the [GitHub repository](https://github.com/hantsy/spring7-sandbox/tree/master/boot-elasticsearch) for the complete implementation.

### CouchBase `Cluster`

Generate a Spring Boot 4 project from [Spring Initializr](http://start.spring.io) with `CouchBase`, `Lombok`, and `Testcontainers` dependencies. Add `org.testcontainers:testcontainers-couchbase` to `pom.xml`. The `spring-boot-starter-couchbase` starter automatically configures a Couchbase `Cluster`. 

Define a record type `Product` to represent documents in a Couchbase bucket:

```java
public record Product(String id,
                      String name,
                      BigDecimal price) {
}
```

Implement a `ProductRepository` to manage documents:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {
    private final Cluster cluster;

    private Collection productCollection;

    @PostConstruct
    public void init() {
        this.productCollection = this.cluster
                .bucket("demo")
                .defaultCollection(); // or collections().createCollection() 
    }

    Product save(Product product) {
        String id = product.id() != null ? product.id() : UUID.randomUUID().toString();
        MutationResult result = this.productCollection.upsert(id, product);
        log.debug("saving product result: {}", result);

        return new Product(id, product.name(), product.price());
    }

    Optional<Product> findById(String id) {
        try {
            GetResult result = this.productCollection.get(id);
            return Optional.of(result.contentAs(Product.class));
        } catch (DocumentNotFoundException e) {
            return Optional.empty();
        }
    }
}
```

Write a test class to validate `ProductRepository` functionality with Couchbase Testcontainers: 

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void testProductRepository() {
        var product = productRepository.save(new Product(null, "test", BigDecimal.ONE));
        assertThat(product).isNotNull();
        assertThat(product.id()).isNotNull();

        Optional<Product> byId = productRepository.findById(product.id());
        assertThat(byId).isPresent();
        var p = byId.get();

        log.debug("found product by id: {}", p);
        assertThat(p.name()).isEqualTo("test");
    }
}
```

View the [GitHub repository](https://github.com/hantsy/spring7-sandbox/tree/master/boot-couchbase) for the complete example. 

### Cassandra `CqlSession`

Begin with a Spring Boot 4 project from [Spring Initializr](http://start.spring.io) featuring `Cassandra`, `Lombok`, and `Testcontainers` dependencies. Add `org.testcontainers:testcontainers-cassandra` to `pom.xml`. The `spring-boot-starter-cassandra` starter automatically configures a Cassandra `CqlSession`. 

Define a record type `Product` to represent Cassandra table rows:

```java
public record Product(String id, String name, BigDecimal price) {}
```

Implement a `ProductRepository` that uses `CqlSession` to interact with Cassandra table data:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {

    private final CqlSession cqlSession;

    Product save(Product product) {
        var id = product.id() != null ? product.id() : UUID.randomUUID().toString();
        String query = """
                INSERT INTO products(id, name, price)
                VALUES (:id, :name, :price)
                """;
        ResultSet resultSet = cqlSession.execute(query, Map.of("id", id,
                "name", product.name(),
                "price", product.price())
        );
        log.debug("saving product: {}", resultSet.wasApplied());

        return new Product(id, product.name(), product.price());
    }

    Optional<Product> findById(String id) {
        String query = """
                SELECT * FROM products WHERE id = :id
                """;
        ResultSet resultSet = cqlSession.execute(query, Map.of("id", id));
        Row one = resultSet.one();
        if (one != null) {
            return Optional.of(
                    new Product(
                            one.get("id", String.class),
                            one.get("name", String.class),
                            one.get("price", BigDecimal.class)
                    )
            );
        }
        return Optional.empty();
    }
}
```

Write a test class to validate functionality using Cassandra Testcontainers: 

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    @Autowired
    ProductRepository productRepository;

    @Test
    public void testProductRepository() {
        var product = productRepository.save(new Product(UUID.randomUUID().toString(), "test", BigDecimal.ONE));
        assertThat(product).isNotNull();
        assertThat(product.id()).isNotNull();

        Optional<Product> byId = productRepository.findById(product.id());
        assertThat(byId).isPresent();
        var p = byId.get();
        log.debug("found product by id: {}", p);
        assertThat(p.name()).isEqualTo("test");
    }

}
```

Explore the [GitHub repository](https://github.com/hantsy/spring7-sandbox/tree/master/boot-cassandra) for the complete implementation.

Unfortunately, Spring Boot doesn't provide a dedicated Redis module because it supports multiple client libraries through Spring Data.

>[!NOTE]
> I also added the reactive variants of the above examples in the Github reporitoty: https://github.com/hantsy/spring-reactive-sample/tree/master

## Summary

Spring Boot 4 splits monolithic starters into fine-grained modules, allowing developers to include only what they need. Each feature provides dedicated `starter` and `starter-test` modules with better performance and reduced overhead.

