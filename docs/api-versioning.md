# API Versioning in Spring 7

API versioning is a practice of managing changes to your API while maintaining backward compatibility.  It allows developers to introduce new features, fix bugs, and refine data structures while providing a stable, predictable experience for users who rely on older versions.

There are several common strategies for API versioning, including:

* URI path versioning (e.g., `/api/v1/resource`) is simple but technically violates some REST principles.
* Query parameter versioning (e.g., `/api/resource?version=1`) is flexible but can lead to cluttered URLs.
* Header versioning (e.g., using a custom header like `X-API-Version: 1`) keeps URLs clean but can be less discoverable.
* Content negotiation (e.g., using the `Accept` header to specify the version) is elegant but can be complex to implement.

Before Spring 7, developers had to implement API versioning manually, which could result in boilerplate code and maintenance challenges. Spring 7 provides built-in features that enable API versioning on both the server side and client side.

## Server-Side API Versioning

In Spring 7, Spring MVC `WebMvcConfigurer` and WebFlux `WebFluxConfigurer` provide a convenient method to configure API versioning.

The following is an example of configuring API versioning within Spring MVC:

```java
@Configuration
class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                // Add version in uri path: /api/v1/resource
                // .usePathSegment(0)
                
                // Set in HTTP header: X-API-Version: 1
                .useRequestHeader("X-API-Version")

                // Add a request parameter: /api/resource?version=1
                // .useRequestParam("version")
                
                // Append version parameter to `Accept` header value: application/json;version=1
                // .useMediaTypeParameter(MediaType.APPLICATION_JSON, "version" )

                .addSupportedVersions("1.0", "1.1", "2.0")

                // default is SemanticApiVersionParser
                //.setVersionParser(new SemanticApiVersionParser())

                // When a defaultVersion is also set, this is automatically set to false.
                // .setVersionRequired(true)
                .setDefaultVersion("1.0");
    }
}
```

In the example above, API versioning is configured to use a custom header `X-API-Version` to specify the API version. You can also use other strategies such as path segments (`usePathSegment`), request headers (`useRequestHeader`), or media type parameters (`useMediaTypeParameter`), or combine them.

The `addSupportedVersions` method specifies all supported versions in the application. Requests using an unsupported version will raise an exception.

By default, Spring uses `SemanticApiVersionParser` to parse version values, which supports semantic versioning (e.g., "1", "1.1", "1.2.3", "v2.0"). You can also implement a custom `ApiVersionParser` for different versioning schemes.

The `setDefaultVersion` method sets a default version when the client does not provide one. The `setVersionRequired` method enforces that clients must specify a version; however, if a `defaultVersion` is set, this is automatically disabled.

>[!NOTE]
> Unfortunately, Spring API versioning does not yet support HATEOAS-compatible content negotiation, such as `Accept: application/vnd.api.v1+json`, though you can implement this manually with Spring HATEOAS. 

The following is an example of REST controller that supports multiple API versions:

```java
@RestController
@RequestMapping("/hello")
class GreetingController {

    @GetMapping()
    public String helloDefault() {
        return "Hello v1.0(Default)";
    }

    @GetMapping(version = "1.1")
    public String helloV1_1() {
        return "Hello v1.1";
    }

    @GetMapping(version = "2.0")
    public String helloV2_0() {
        return "Hello v2.0";
    }
}
```

The `RequestMapping` annotation and derived annotations such as `GetMapping` support a `version` attribute to specify the API version for each handler method. When a request includes the appropriate version in the header, the corresponding method is invoked. For example, `X-API-Version: 1.1` invokes the `helloV1_1` method, whereas `X-API-Version: 2.0` invokes the `helloV2_0` method. If no version is specified, the `helloDefault` method is invoked as the default (v1.0).

Check the full example codes of [Spring WebMVC](https://github.com/hantsy/spring7-sandbox/tree/master/boot-api-versioning-webmvc) and [Spring WebFlux](https://github.com/hantsy/spring7-sandbox/tree/master/boot-api-versioning-webflux).

You can configure API versioning in both Spring MVC and WebFlux via application properties as well in a Spring Boot 4 project, for example:

```properties
spring.mvc.api-versioning.use-request-header=true
spring.mvc.api-versioning.request-header-name=X-API-Version
spring.mvc.api-versioning.supported-versions=1.0,1.1,2.0
spring.mvc.api-versioning.default-version=1.0
```

## Client-Side API Versioning

Spring 7 also adds API versioning support to Spring's HTTP clients, including `RestClient`, the reactive `WebClient`, and test-focused tools like `MockMvc/RestTestClient` and `WebTestClient`.

The following is a test class using `RestClient` to verify the REST endpoints shown above.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

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
                //.apiVersion(1.0)
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
```

The `RestClient` is built on the JDK HTTP engine and configured with a default version and a custom `ApiVersionInserter` to insert the API version into the request header. The `apiVersion` method specifies the API version for each request; if not specified, it defaults to v1.0.

The following example uses `MockMvc` to test API versioning in Spring MVC:

```java
@WebMvcTest(controllers = GreetingController.class)
public class GreetingControllerTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        MockMvcBuilderCustomizer mockMvcBuilderCustomizer() {
            return builder -> builder
                    .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                    .build();
        }
    }

    @Autowired
    MockMvc mockMvc;
    
    @Test
    void testHello() throws Exception {
        this.mockMvc.perform(get("/hello")/*.apiVersion("1.0")*/)
                .andExpect(content().string("Hello v1.0(Default)"));
    }

    @Test
    void testHello1_1() throws Exception {
        this.mockMvc.perform(get("/hello").apiVersion("1.1"))
                .andExpect(content().string("Hello v1.1"));
    }

    @Test
    void testHello2_0() throws Exception {
        this.mockMvc.perform(get("/hello").apiVersion("2.0"))
                .andExpect(content().string("Hello v2.0"));
    }
}
```

Through the `MockMvcBuilderCustomizer`, we can configure the `MockMvc` instance to use a custom `ApiVersionInserter` to insert the API version into the request header.

The following is an example of using `RestTestClient` to test the API versioning in Spring MVC:

```java
@WebMvcTest(controllers = GreetingController.class)
public class GreetingControllerWithRestTestClientTest {

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
```

In this example, the `RestTestClient` is bound to the `MockMvc` instance and configured with a default version and a custom `ApiVersionInserter`.

Explore the full example codes using reative [WebClient](https://github.com/hantsy/spring7-sandbox/blob/master/boot-api-versioning-webflux/src/test/java/com/example/demo/DemoApplicationTests.java) and [WebTestClient](https://github.com/hantsy/spring7-sandbox/blob/master/boot-api-versioning-webflux/src/test/java/com/example/demo/GreetingControllerTest.java) in the GitHub repository.
