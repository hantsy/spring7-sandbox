# Programmatic Bean Registration with BeanRegistrar

Prior to Spring Framework 7, beans could be registered programmatically in several ways — for example, by implementing `BeanDefinitionRegistryPostProcessor` or `ImportBeanDefinitionRegistrar`, or by manipulating the `ApplicationContext` directly.

A custom `BeanDefinitionRegistryPostProcessor` might look like this:

```java
public class MyBeanRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        BeanDefinitionBuilder postRepositoryBeanDef = BeanDefinitionBuilder.genericBeanDefinition(PostRepository.class);
        registry.registerBeanDefinition("posts", postRepositoryBeanDef.getBeanDefinition());
    }
}
```

This approach is somewhat verbose and requires familiarity with Spring internals.

Another example is to register beans directly with an `AnnotationConfigApplicationContext` which is introduced in Spring 5:

```java
AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
PostRepository posts = new PostRepository();
PostHandler postHandler = new PostHandler(posts);
Routes routesBean = new Routes(postHandler);

context.registerBean(PostRepository.class, () -> posts);
context.registerBean(PostHandler.class, () -> postHandler);
context.registerBean(Routes.class, () -> routesBean);
context.registerBean(WebHandler.class, () -> RouterFunctions.toWebHandler(routesBean.routes(), HandlerStrategies.builder().build()));
context.refresh();
```

Here is the complete project of the above code snippet, see [spring-reactive-sample/register-bean](https://github.com/hantsy/spring-reactive-sample/tree/master/register-bean).

However, this technique also entails considerable boilerplate.

Spring Framework 7 introduces a more concise API for programmatic bean registration via a `BeanRegistrar` interface. For example, you might have two implementations of a `PostRepository` interface — one backed by R2DBC and another using an in-memory alternative.

The following example demonstrates conditional registration based on the active profile using `BeanRegistrar`:

```java
class MyBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(BeanRegistry registry, Environment env) {
        if (env.matchesProfiles("h2")) {
            // registry.registerBean(R2dbcConfig.class);
            registry.registerBean(
                    PostRepository.class,
                    (BeanRegistry.Spec<PostRepository> spec) -> spec
                            .supplier(ctx -> new H2PostRepository(ctx.bean(DatabaseClient.class)))
            );
        } else {
            registry.registerBean(
                    PostRepository.class,
                    (BeanRegistry.Spec<PostRepository> spec) -> spec
                            .supplier(ctx -> new InMemoryPostRepository())
            );
        }

        registry.registerBean(PostHandler.class,
                (BeanRegistry.Spec<PostHandler> spec) -> spec
                        .supplier(ctx -> new PostHandler(ctx.bean(PostRepository.class)))
        );

        registry.registerBean(Routes.class,
                (BeanRegistry.Spec<Routes> spec) -> spec
                        .supplier(ctx -> new Routes(ctx.bean(PostHandler.class))));

        registry.registerBean("webHandler", WebHandler.class,
                (BeanRegistry.Spec<WebHandler> spec) -> spec
                        .prototype()
                        .supplier(ctx -> RouterFunctions.toWebHandler(ctx.bean(Routes.class).routes(), HandlerStrategies.builder().build()))
        );

    }
}
```

Import this custom `MyBeanRegistrar` into a configuration class to activate it:

```java
@Configuration
@Import(MyBeanRegistrar.class)
public class CustomConfig {
}
```

`BeanRegistrar` provides fine-grained control over bean registration, including scope (singleton, prototype, etc.), lazy initialization, and custom suppliers for instantiation. It is also compatible with native-image compilation using GraalVM.

Annotating the test class with `@ActiveProfiles("h2")` activates the h2 profile, causing the `H2PostRepository` implementation to be registered and injected.

```java
@SpringJUnitConfig(classes = {CustomConfig.class, R2dbcConfig.class})
@ActiveProfiles("h2")
class H2ApplicationTest {

    @Autowired
    PostRepository postRepository;

    @Test
    void testPostRepository() {
        assertThat(this.postRepository).isInstanceOf(H2PostRepository.class);
    }


    @Test
    void testCurdOperations() {
        Post post = new Post(null, "Test Title", "Test Content");
        Mono<UUID> savedPostId = this.postRepository.save(post);

        savedPostId.flatMap(id -> this.postRepository.findById(id))
                .as(StepVerifier::create)
                .consumeNextWith(p -> {
                    assertThat(p.title()).isEqualTo("Test Title");
                    assertThat(p.content()).isEqualTo("Test Content");
                })
                .verifyComplete();

        savedPostId.flatMap(id ->
                        this.postRepository.update(id, new Post(null, "Updated Title", "Updated Content"))
                )
                .subscribe(updatedCount -> System.out.println("Updated rows: " + updatedCount));

        this.postRepository.findAll().subscribe(System.out::println);

        savedPostId.flatMap(id -> this.postRepository.deleteById(id))
                .subscribe(System.out::println);

        this.postRepository.findAll().subscribe(System.out::println);
    }
}
```

Elsewhere, the `InMemoryPostRepository` will be used instead.

```java
@SpringJUnitConfig(CustomConfig.class)
class ApplicationTest {

    @Autowired
    PostRepository postRepository;

    @Test
    void testPostRepository() {
        assertThat(this.postRepository).isInstanceOf(InMemoryPostRepository.class);
    }
}
```

Spring 7 additionally offers `BeanRegistrarDsl`, a Kotlin extension that enables a more fluent registration style with Kotlin DSL.

Here is an example written in Kotlin and utilizing `BeanRegistrarDsl`:

```kotlin
@Configuration
@Import(MyBeanRegistrar::class)
class CustomConfig

class MyBeanRegistrar : BeanRegistrarDsl({
    registerBean { InMemoryPostRepository() }
    registerBean { PostHandler(bean()) }
    registerBean { Routes(bean()) }

    registerBean(
        name = "webHandler",
        backgroundInit = true,
        prototype = false,
        supplier = {
            RouterFunctions.toWebHandler(bean<Routes>().routes(), HandlerStrategies.builder().build())
        }
    )
    register(FoobarRegistrar())
})

class FoobarRegistrar : BeanRegistrarDsl({
    profile("foo") {
        registerBean<Bar> { Bar(bean<Foo>()) }
        registerBean { Foo() }
    }
})

class Bar(foo: Foo)
class Foo
```

See the repository examples for complete samples:

- https://github.com/hantsylabs/spring7-sandbox/tree/main/bean-registrar
- https://github.com/hantsylabs/spring7-sandbox/tree/main/bean-registrar-kotlin-dsl

