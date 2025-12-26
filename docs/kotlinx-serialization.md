# First-Class Kotlin Serialization Support in Spring Boot 4

Prior to Spring Boot 4, Spring Boot provided JSON serialization and deserialization support via Jackson, Gson, and Jakarta JSON-B; these libraries were auto-configured and worked out of the box. Kotlinx Serialization JSON was an exception—Spring Boot did not provide comparable auto-configuration. Kotlinx Serialization was only enabled when Kotlin classes were annotated with `@Serializable` and the `kotlinx-serialization-json` dependency was present on the classpath. When Jackson and Kotlinx Serialization were both present, Spring Boot generally used Jackson as the default JSON processor; however, for certain types (for example, enums or collections of enums) processing could unexpectedly switch to Kotlinx Serialization (see: [spring-boot#24238](https://github.com/spring-projects/spring-boot/issues/24238)).

With Spring Boot 4, Kotlinx Serialization JSON is now a first-class citizen. As with Jackson, Gson, and Jakarta JSON-B, [the modularized Spring Boot distribution](https://spring.io/blog/2025/10/28/modularizing-spring-boot) provides a standalone starter `spring-boot-starter-kotlinx-serialization-json`. Including this starter in a Kotlin project causes Spring Boot to enable Kotlinx Serialization support and expose the `kotlinx.serialization.json.Json` bean with default settings.

Let's create a simple Spring Boot 4 Kotlin project to demonstrate Kotlinx Serialization support.

Create a Spring Boot project via https://start.spring.io/ with the following options:

* Language: Kotlin
* Spring Boot: 4.0.1
* Project type: Maven
* Project metadata:
  * Java version: 25
  * Keep the other fields at their default values.

Generate the project, extract the downloaded ZIP, and import it into your IDE.

Next, add the `spring-boot-starter-kotlinx-serialization-json` and `spring-boot-starter-kotlinx-serialization-json-test` to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-kotlinx-serialization-json</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-kotlinx-serialization-json-test</artifactId>
    <scope>test</scope>
</dependency>
```

The `spring-boot-starter-kotlinx-serialization-json` starter includes an auto-configuration class that registers a `Json` bean preconfigured with sensible defaults.

In your `pom.xml`, find the `kotlin-maven-plugin` and add the `kotlinx-serialization` plugin to its `configuration` section, then add the `org.jetbrains.kotlin:kotlin-maven-serialization` dependency to the plugin's `dependencies` section:

```xml
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <version>${kotlin.version}</version>
    <configuration>
        <compilerPlugins>
            ...
            <plugin>kotlinx-serialization</plugin>
        </compilerPlugins>
    </configuration>
    <dependencies>
        ...
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-serialization</artifactId>
            <version>${kotlin.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

With this plugin, the Kotlin compiler processes `@Serializable` annotations at compile time.

> [!NOTE]
> For Gradle users, refer to the [Kotlinx Serialization documentation](https://github.com/Kotlin/kotlinx.serialization?tab=readme-ov-file#gradle) to configure the corresponding Gradle plugin.

Now, let's create a simple data class annotated with `@Serializable`:

```kotlin
@Serializable
data class Message(val body: String)
```

Build the project (on Unix-like systems):

```bash
./mvnw clean compile
```

On Windows, use `mvnw.cmd clean compile`.

Check the compiled class for `Message`, open it in your IDE, or using `javap` command to see the contents:

```java 
@kotlinx.serialization.Serializable public final data class Message public constructor(body: kotlin.String) {
    public companion object {
        public final fun serializer(): kotlinx.serialization.KSerializer<com.example.demo.Message> { /* compiled code */ }
    }
    //...

    @kotlin.Deprecated public object `$serializer` : kotlinx.serialization.internal.GeneratedSerializer<com.example.demo.Message> {
        public final val descriptor: kotlinx.serialization.descriptors.SerialDescriptor /* compiled code */

        public final fun childSerializers(): kotlin.Array<kotlinx.serialization.KSerializer<*>> { /* compiled code */ }

        public final fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): com.example.demo.Message { /* compiled code */ }

        public final fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: com.example.demo.Message): kotlin.Unit { /* compiled code */ }
    }
}
```

As shown, the Kotlin compiler generates a companion object with a `serializer()` method and a `$serializer` object that implements `KSerializer`. These artifacts handle serialization and deserialization for the `Message` class.

Next let's create a simple test to use `Json` bean to serialize and deserialize the `Message` class:

```kotlin
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    lateinit var json: Json

    @Test
    fun testMessage() {
        val encodedMessage = json.encodeToString(Message("hello world"))
        println("encoded message: $encodedMessage")
        assertEquals(
            """
            {
                "body": "hello world"
            }
        """.trimIndent(),
            encodedMessage
        )

        val decodedMessage = json.decodeFromString<Message>(encodedMessage)
        println("decoded message: $decodedMessage")
    }
}
```

Spring Boot also provides a `KotlinxSerializationJsonBuilderCustomizer` interface to customize `kotlinx.serialization.json.JsonBuilder`, which is used to construct the `Json` bean.

For example, you can create a `KotlinxSerializationJsonBuilderCustomizer` bean like this:

```kotlin
@OptIn(ExperimentalSerializationApi::class)
@Configuration(proxyBeanMethods = false)
class CustomConfig {

    @Bean
    fun kotlinxSerializationJsonBuilderCustomizer(): KotlinxSerializationJsonBuilderCustomizer {
        return KotlinxSerializationJsonBuilderCustomizer { builder ->
            builder.apply {
                namingStrategy = JsonNamingStrategy.SnakeCase
                prettyPrint = true
                explicitNulls = false
                encodeDefaults = true
                ignoreUnknownKeys = false
            }
        }
    }

}
``` 

In this example, the `Json` bean is configured to use a snake_case naming strategy, enable pretty printing, and set other serialization options.

Alternatively, you can also set these properties via `application.properties` or `application.yml`:

```properties
spring.kotlinx.serialization.json.naming-strategy=snake_case
spring.kotlinx.serialization.json.pretty-print=true
spring.kotlinx.serialization.json.explicit-nulls=false
spring.kotlinx.serialization.json.encode-defaults=true
spring.kotlinx.serialization.json.ignore-unknown-keys=false
```

> [!NOTE]
> All available configuration properties can be found in the [`KotlinxSerializationJsonProperties`](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/kotlinx/serialization/json/autoconfigure/KotlinxSerializationJsonProperties.html) class.

Let's have a look at another example. 

```kotlin 
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Person(
    @JsonNames("full_name") val name: String,

    @Serializable(LocalDateSerializer::class)
    val birthDate: LocalDate,

    val gender: Gender = Gender.MALE
)

enum class Gender {
    MALE, FEMALE;
}

class LocalDateSerializer : KSerializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): LocalDate {
        val string = decoder.decodeString()
        return LocalDate.parse(string, formatter)
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        val result = value.format(formatter)
        encoder.encodeString(result)
    }
}
```

In this example we define a `Person` data class, the `birthDate` type is `LocalDate`, which is from the Java Date and Time API and not supported by Kotlinx Serialization. Here we create a custom `LocalDateSerializer` to overcome this issue. While `@SerialName` renames a property in the JSON output, `@JsonNames("full_name")` specifies alternative field names to accept during deserialization.

> [!NOTE]
> Kotlinx Serialization is designed for Kotlin multiplatform, it has built-in support for [Kotlin/kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime).

Create a test to verify `Person` serialization and deserialization:

```kotlin
    @Test
    fun testKotlinxSerializationJson() {
        val encodedPerson = json.encodeToString(Person("Hantsy Bai", LocalDate.of(1970, 1, 1)))
        println("encoded person: $encodedPerson")

        assertEquals(
            """
            {
                "name": "Hantsy Bai",
                "birth_date": "1970-01-01",
                "gender": "MALE"
            }
        """.trimIndent(), encodedPerson
        )

        val decodedPerson = json.decodeFromString<Person>(encodedPerson)
        println("decoded person: $decodedPerson")
        assertEquals(Gender.MALE, decodedPerson.gender)
    }
```

The following test verifies deserialization when alternative field names are used and default values are applied:

```kotlin
    @Test
    fun testWithFullnameAndDefault() {

        val jsonPerson = """
             {
                "full_name": "Hantsy Bai",
                "birth_date": "1970-01-01"
            }
        """.trimIndent()

        val decodedPerson2 = json.decodeFromString<Person>(jsonPerson)
        println("decoded person: $decodedPerson2")
        assertEquals("Hantsy Bai", decodedPerson2.name)
        assertEquals(Gender.MALE, decodedPerson2.gender)
    }
```

In this test we provide `full_name` instead of `name` and omit `gender` to verify that the default is applied during deserialization.

In Spring Boot 3.x Web MVC and WebFlux applications, mixing multiple JSON libraries sometimes required explicitly selecting the preferred mapper via `spring.http.converters.preferred-json-mapper` (for MVC) or `spring.http.codecs.preferred-json-mapper` (for WebFlux). Spring Boot 4's modular starters largely resolve these conflicts: unless the corresponding starter is present on the classpath, its auto-configuration will not be imported.