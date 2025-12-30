# An Introduction to Jackson 3 in Spring 7 and Spring Boot 4

Jackson is the de facto standard for JSON processing in Spring apps. With Spring 7 and Spring Boot 4, Jackson 3 is now the default — it modernizes the codebase for Java 17+ and also brings some breaking changes. Let’s walk through what changed and what you need to do when come to Spring 7 world.

[!NOTE]
> Check out the release notes and migration guide: [Jackson-Release-3.0](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0) and the [Migration Guide](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md).

If you're migrating from Spring 6 to Spring 7, you'll probably need to update your Jackson configuration — here's one example of Jackson 2 configuration in Spring 6:

```java
@Configuration
public class Jackson2ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {

        var builder = Jackson2ObjectMapperBuilder.json();
        builder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
        builder.featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                SerializationFeature.FAIL_ON_EMPTY_BEANS,
                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        builder.featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        builder.modulesToInstall(JavaTimeModule.class);

        return builder.build();
    }
}
```

The above code can be replaced with the following code using Jackson 3 in Spring 7:

```java
@Configuration(proxyBeanMethods = false)
class JacksonJsonMapperConfig {

    @Bean
    JsonMapper jacksonJsonMapper() {
        var builder = JsonMapper.builder();

        builder.changeDefaultPropertyInclusion(include -> include.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .findAndAddModules();

        return builder.build();
    }
}
```

A few notes on the example:

* In Spring 7 you'll use the Jackson `Builder` to build up the configuration.
* Jackson 3 reworks `ObjectMapper` and introduces `JsonMapper` for JSON work. There are also specialized mappers like `YamlMapper` and `XmlMapper` for other formats.
* Dates and times are serialized as ISO‑8601 strings by default, so you don't need to turn off `WRITE_DATES_AS_TIMESTAMPS`.

>[!NOTE]
> Heads up: even though Jackson 3 uses `tools.jackson`, it still shares the [`jackson-annotations`](https://github.com/FasterXML/jackson-annotations) module with Jackson 2, which uses the legacy namespace `com.fasterxml.jackson`. It looks odd, but this is intentional for backward compatibility.

Like the Jackson 2 `Jackson2ObjectMapperBuilderCustomizer` in Spring Boot 3.x, Spring Boot 4 gives you a `JsonMapperBuilderCustomizer` hook to tweak Jackson 3's configuration.

Let's kick off a simple Spring Boot 4 project to try out Jackson 3.

Start a new project at [Spring Initializr](https://start.spring.io/) with these selections:

* Project: Maven
* Language: Java
* Spring Boot: 4.0.0
* Project Metadata/Java: 25
* Leave the other fields as-is.

Download and unzip the project, then open it in your favorite IDE.

Add `spring-boot-starter-jackson` and `spring-boot-starter-jackson-test` dependencies to your `pom.xml`:

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

The `spring-boot-starter-jackson` gives you a Jackson 3 `JsonMapper` with sensible defaults. If you want to change those defaults, provide a `JsonMapperBuilderCustomizer` bean.

```java
@Bean
JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
    return builder -> builder
            .changeDefaultPropertyInclusion(value -> value.withContentInclusion(Include.NON_NULL))
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();
}
```

Or set these via `application.properties`:

```properties
spring.jackson.default-property-inclusion=non_null
spring.jackson.serialization.indent-output=true
``` 

The `spring-boot-starter-jackson-test` sets up a minimal JSON test context for use with `@JsonTest`. Inject `JacksonTester` into your tests to assert JSON serialization and deserialization.

Here's a simple `Person` example you can use in tests.

```java
record Person(
        String name,
        LocalDate birthDate,
        Gender gender
) {
}

enum Gender {
    MALE,
    FEMALE
}

@JacksonComponent
class CustomLocalDateSerializer {

    static DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static class LocalDateSerializer extends ValueSerializer<LocalDate> {

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeString(value.format(LOCAL_DATE_FORMAT));
        }
    }

    static class LocalDateDeserializer extends ValueDeserializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            return LocalDate.parse(ctxt.readValue(p, String.class), LOCAL_DATE_FORMAT);
        }
    }
}

@JacksonMixin(Person.class)
record MyPerson(@JsonProperty("fullName") String name) {
}
```

This example adds a custom serializer/deserializer for `LocalDate` and a mixin that maps `name` to `fullName` in JSON.

Now let's create a test class to verify serialization and deserialization of `Person`.

```java
@JsonTest
class JacksonTests {
    private static final Logger log = LoggerFactory.getLogger(JacksonTests.class);

    @Autowired
    JacksonTester<Person> tester;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    void testPersonSerialAndDeserial() throws IOException {
        var data = new Person(
                "Hantsy Bai",
                LocalDate.of(1970, 1, 1),
                Gender.MALE
        );

        var jsonData = jsonMapper.writeValueAsString(data);
        log.debug("serialized json string: {} ", jsonData);

        var testContents = tester.parse(jsonData);
        testContents.assertThat()
                .matches(person -> person.name().equals("Hantsy Bai"));
    }
}
```

The test serializes a `Person` to JSON string with `JsonMapper`, and parses it with `JacksonTester`, and then asserts the `name` property.

The next test shows the mixin is applied during deserialization and that the `name` field is populated as expected.

```java
@Test
void deserializedWithFullName() throws IOException {
    var jsonData = """
            {
                "fullName":"Hantsy Bai",
                "birthDate":"01/01/1970",
                "gender":"MALE"
            }
            """.trim();

    tester.parse(jsonData)
            .assertThat()
            .hasFieldOrProperty("name")
            .matches(person -> person.name().equals("Hantsy Bai"));

}
```

If you still need Jackson 2, that's fine — add the Jackson 2 modules (`jackson-module-parameter-names`, `jackson-datatype-jdk8`, `jackson-datatype-jsr310`) and set `spring.http.converters.preferred-json-mapper=jackson2` (Web MVC) or `spring.http.codecs.preferred-json-mapper=jackson2` (WebFlux) to return back to use Jackson 2 for JSON serialization and deserialization.

One gotcha I hit during migration: `Locale` serialization changed. For example, `Locale.CHINA` used to serialize as `zh_CN` in Jackson 2 but is `zh-CN` in Jackson 3 — Jackson 3 now uses the `LanguageTag` format, so be aware of the difference when you parse or compare locale values.

Grab the complete example on GitHub: [spring7-sandbox/boot-jackson](https://github.com/hantsy/spring7-sandbox/tree/master/boot-jackson).
