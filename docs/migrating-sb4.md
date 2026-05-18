# Migrating to Spring Boot 4.0: Challenges and Best Practices

Unlike the transition from Spring Boot 2 to 3, which was relatively straightforward, Spring Boot 4.0 introduces several architectural shifts and breaking changes that require more significant migration effort.

## Migration Challenges and Tips

First, **Jackson 3** is now the default JSON codec. This version introduces new namespaces and removes many APIs that were deprecated in Jackson 2. Migrating legacy code requires replacing `ObjectMapper` configurations with the modern `JsonMapper` builder API. Additionally, you must update imports to use the new `tools.jackson` package instead of the legacy `com.fasterxml.jackson` namespace.

For a deep dive into these changes, see [An Introduction to Jackson 3 in Spring 7 and Spring Boot 4](./jackson.md).

Second, Spring Boot 4.0 adopts a **modular approach** for finer control over dependencies. Web clients, in particular, now have their own dedicated starters. For example, if your application only uses `WebClient` or `RestClient`, you can now include `spring-boot-starter-webclient` or `spring-boot-starter-restclient` instead of pulling in the entire `spring-boot-starter-webflux` or `spring-boot-starter-web` stack. Furthermore, test starters are now feature-specific; using `@WebClientTest` now requires adding the `spring-boot-starter-webclient-test` dependency.

You can learn more about these changes in our guide on [Modularization](./modularization.md).

Spring 7 and Spring Boot 4 are aligned with **Jakarta EE 11**, which includes Jakarta Persistence 3.2. A notable improvement in Spring 7 is the enhanced JPA support that allows for the direct injection of `EntityManager` and `EntityManagerFactory` into Spring beans using standard `@Autowired` or constructor injection, eliminating the strict requirement for `@PersistenceContext` or `@PersistenceUnit`.

Testing also sees a significant update with the move to **JUnit 6**. The legacy `@MockBean` and `@SpyBean` annotations have been replaced by `@MockitoBean` and `@MockitoSpy`. While the new annotations provide a more consistent experience, they require a manual update of your test suites and imports.

## Migration Tools and Resources

To facilitate a smoother transition, **OpenRewrite** provides recipes for Spring Boot 4.0 migration via Maven and Gradle plugins. These tools can automatically apply many of the required code changes. You can find more details in the [Spring Boot 4.0 Migration Guide (Community Edition)](https://docs.openrewrite.org/recipes/java/spring/boot4/upgradespringboot_4_0-community-edition). For enterprise-scale migrations, Moderne offers commercial support and advanced CLI tools, check [How to speed and scale your Spring Boot 4 migration](https://www.moderne.ai/blog/spring-boot-4x-migration-guide).

In a recent project using **Kotlin**, we found that the standard OpenRewrite recipes occasionally struggled with Kotlin-specific syntax. In such cases, a manual migration was necessary, which provided a valuable opportunity to explore the nuances of Spring Boot 4.0 firsthand.

## A Note on Java 25 Compatibility

During our migration to Spring Boot 4 and **Java 25**, we encountered a peculiar issue related to the new `ClassFile` API introduced in Java 23. Spring 7 uses this API by default for bean scanning when running on modern JVMs. However, in certain Java 25 environments, this led to unexpected **OutOfMemory (OOM)** errors.

I've detailed [this experience on StackOverflow](https://stackoverflow.com/questions/79860529/spring-boot-4-and-java-25). As a temporary workaround, we implemented a custom `MetadataReaderFactoryDelegate` to override the default scanning behavior and restore to use the classic cglib/asm scanning approach until the underlying implementation is further optimized.
