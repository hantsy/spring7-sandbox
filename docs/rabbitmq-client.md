# An Introduction to the New Spring RabbitMQ Client

Spring AMQP 4.0 brings a new module `spring-rabbitmq-client` which is based on the new RabbitMQ official Java client `com.rabbitmq.client:amqp-client`, which is aligned with AMQP 1.0 protocol. When using `spring-rabbitmq-client` in your projects, it is recommended to upgrade to RabbitMQ 4.0 for native AMQP 1.0 support.

!!! note
    AMQP 1.0 is not compatible with AMQP 0.91. Version 1.0 is not a simple upgrade from 0.91. The two versions can be considered as two different protocols. RabbitMQ 3.x supports both protocols, but enabling AMQP 1.0 support requires installing an extra plugin `rabbitmq_amqp1_0`. RabbitMQ 4.0 switches to AMQP 1.0 as the default core protocol. The previous Spring RabbitMQ module `spring-rabbit` is still based on the AMQP 0.9.1 protocol, and Spring Boot 4.2 renamed its RabbitMQ (AMQP 0.9) support to `spring-boot-rabbitmq`. The generic `spring-amqp-client` module implements the AMQP 1.0 protocol for any compliant broker, see: [spring-amqp#3271](https://github.com/spring-projects/spring-amqp/issues/3271)

## Getting Started

Create a new simple Maven project with the basic `spring-core` and `spring-context` as dependencies, or generate a simple Spring Boot project via https://start.spring.io as [the previous post](https://github.com/hantsy/spring7-sandbox/blob/master/docs/jackson.md).

!!! note
    Spring Boot 4.2 ships starters for the AMQP 1.0 support: `spring-boot-starter-amqp` provides generic AMQP 1.0 support, and `spring-boot-starter-amqp-rabbitmq` provides the AMQP 1.0 support tailored for RabbitMQ. A plain Spring project like this one still has to declare the dependency and the configuration manually; see [Spring Boot Integration](#spring-boot-integration) for the auto-configured setup.

Then add the following dependency in your `pom.xml` file:

```xml
<dependencies>
    ...
    <dependency>
        <groupId>org.springframework.amqp</groupId>
        <artifactId>spring-rabbitmq-client</artifactId>
    </dependency>
</dependencies>
```

This project uses `spring-boot-starter-parent` as its parent POM for dependency management only, so the version of `spring-rabbitmq-client` is managed by Spring Boot.

Next, let's create a simple configuration class:

```java
@Configuration
@EnableRabbit
public class RabbitClientConfig {

    @Value("${rabbitmq.port:5672}")
    private int port;

    @Bean
    Environment environment() {
        return new AmqpEnvironmentBuilder()
                .connectionSettings()
                .port(port)
                .environmentBuilder()
                .build();
    }

    @Bean
    AmqpConnectionFactory amqpConnectionFactory(Environment environment) {
        return new SingleAmqpConnectionFactory(environment);
    }

    @Bean
    RabbitAmqpAdmin rabbitAmqpAdmin(AmqpConnectionFactory connectionFactory) {
        return new RabbitAmqpAdmin(connectionFactory);
    }
    
    @Bean
    RabbitAmqpTemplate rabbitAmqpTemplate(AmqpConnectionFactory connectionFactory) {
        RabbitAmqpTemplate rabbitAmqpTemplate = new RabbitAmqpTemplate(connectionFactory);
        return rabbitAmqpTemplate;
    }

    @Bean(RabbitListenerAnnotationBeanPostProcessor.DEFAULT_RABBIT_LISTENER_CONTAINER_FACTORY_BEAN_NAME)
    RabbitAmqpListenerContainerFactory rabbitAmqpListenerContainerFactory(AmqpConnectionFactory connectionFactory) {
        RabbitAmqpListenerContainerFactory factory = new RabbitAmqpListenerContainerFactory(connectionFactory);
        return factory;
    }
}
```

In the above configuration class, we create the essential beans, eg, `AmqpConnectionFactory`, `RabbitAmqpAdmin`, `RabbitAmqpTemplate`, and `RabbitAmqpListenerContainerFactory` for the Spring RabbitMQ client. Make sure the `Environment` and `AmqpEnvironmentBuilder` are from the package `com.rabbitmq.client.amqp`.

With `RabbitAmqpAdmin`, you can declare exchanges, queues, and bindings as usual:

```java
public class RabbitClientConfig {
    public final static String HELLO_EXCHANGE_NAME = "e1";
    public final static String HELLO_QUEUE_NAME = "q1";
    public final static String HELLO_ROUTING_KEY = "k1";
    //...
    @Bean
    DirectExchange helloExchange() {
        return ExchangeBuilder
                .directExchange(HELLO_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    Queue helloQueue() {
        return QueueBuilder.durable(HELLO_QUEUE_NAME)
                .deadLetterExchange("dlx1")
                .build();
    }

    @Bean
    Binding helloBinding() {
        return BindingBuilder
                .bind(helloQueue())
                .to(helloExchange())
                .with(HELLO_ROUTING_KEY);
    }
}
```    

To bootstrap a RabbitMQ server at runtime, add the following testcontainers dependencies in your `pom.xml`:

```xml
 <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-rabbitmq</artifactId>
    <scope>test</scope>
</dependency> 
```   

Create a `ApplicationContextInitializer` to start the RabbitMQ container before Spring context is initialized:

```java
class RabbitContainerInitializer implements ApplicationContextInitializer<@NotNull ConfigurableApplicationContext> {
    private static final Logger log = LoggerFactory.getLogger(RabbitContainerInitializer.class);
    final static String DOCKER_IMAGE_NAME = "rabbitmq:4-management-alpine";
    final RabbitMQContainer container = new RabbitMQContainer(DockerImageName.parse(DOCKER_IMAGE_NAME))
            .withExposedPorts(5672, 15672, 5552);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        container.start();
        applicationContext.addApplicationListener(e -> {
            if (e instanceof ContextClosedEvent) {
                container.stop();
            }
        });
        log.debug("RabbitMQ container exposed ports:" + container.getFirstMappedPort());
        applicationContext.getEnvironment()
                .getPropertySources()
                .addLast(
                        new MapPropertySource("rabbitProps",
                                Map.of("rabbitmq.port", container.getFirstMappedPort())
                        )
                );
    }
}
```

!!! note
    Make sure you are using RabbitMQ 4.x image to get AMQP 1.0 protocol support by default.

Now create a test to verify sending and receiving messages:

```java
@SpringJUnitConfig(classes = {
        RabbitAmqpTemplateTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class RabbitAmqpTemplateTest {

    @Configuration
    @Import({RabbitClientConfig.class})
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Test
    void testSendAndReceive() throws Exception {
        assertThat(this.rabbitAmqpTemplate.convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, "test1"))
                .succeedsWithin(Duration.ofSeconds(10));

        assertThat(this.rabbitAmqpTemplate.receiveAndConvert(HELLO_QUEUE_NAME))
                .succeedsWithin(Duration.ofSeconds(10))
                .isEqualTo("test1");
    }
}
```

The `RabbitAmqpTemplate` implements `AsyncAmqpTemplate`, so all send and receive operations are asynchronous and return `CompletableFuture`.

The `RabbitAmqpTemplate` also supports RPC-style messaging via the `convertSendAndReceive` method:

```java
@Test
void verifyRpc() {
    String testRequest = "rpc-request";
    String testReply = "rpc-reply";

    CompletableFuture<Object> rpcClientResult = this.rabbitAmqpTemplate.convertSendAndReceive("e1", "k1", testRequest);

    AtomicReference<String> receivedRequest = new AtomicReference<>();
    CompletableFuture<Boolean> rpcServerResult =
            this.rabbitAmqpTemplate.<String, String>receiveAndReply("q1",
                    payload -> {
                        receivedRequest.set(payload);
                        return testReply;
                    });

    assertThat(rpcServerResult).succeedsWithin(Duration.ofSeconds(10)).isEqualTo(true);
    assertThat(rpcClientResult).succeedsWithin(Duration.ofSeconds(10)).isEqualTo(testReply);
    assertThat(receivedRequest.get()).isEqualTo(testRequest);
}
```

We have configured a listener container bean `RabbitAmqpListenerContainerFactory` in the configuration class. Now you can use `@RabbitListener` to consume messages as usual:

```java
@Component
class HelloListener {
    private static final Logger log = LoggerFactory.getLogger(HelloListener.class);
    final List<String> received = Collections.synchronizedList(new ArrayList<>());

    @RabbitListener(queues = {HELLO_QUEUE_NAME}, id = "testHelloListener")
    void processHello(String data) {
        log.debug(":: received data: [{}]", data);
        this.received.add(data);
    }
}
```

The `HelloListener` component will receive messages sent to the `HELLO_QUEUE_NAME` queue, and save them in the `received` list.

Let's write a test to verify the listener works as expected:

```java
@SpringJUnitConfig(classes = {
        HelloListenerContainerTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class HelloListenerContainerTest {
    private final Logger log = LoggerFactory.getLogger(HelloListenerContainerTest.class);

    @Configuration
    @Import({
            RabbitClientConfig.class,
            HelloListener.class
    })
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Autowired
    HelloListener listener;

    @Test
    void testHello() {
        log.debug("Start sending message...");
        var initialFuture = CompletableFuture.completedFuture(true);
        var words = List.of(
                "the",
                "quick",
                "dog",
                "jumped",
                "over",
                "the",
                "lazy",
                "fox");
        for (String word : words) {
            initialFuture = initialFuture
                    .thenComposeAsync(_ -> rabbitAmqpTemplate
                            .convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, word)
                            .whenComplete((res, ex) -> log.debug("sent: [{}]", word))
                    );
        }

        initialFuture.join();

        Awaitility.await().atMost(Duration.ofMillis(1_000))
                .untilAsserted(() -> {
                    List<String> received = this.listener.received;
                    log.debug(">>> ackListener received: {}", received);

                    Map<String, Long> wordCount = received.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                    log.debug("word count: {}", wordCount);

                    assertThat(wordCount.get("the")).isEqualTo(2);
                });

    }
}
```

By default, the message is sent and acknowledged automatically. You can add fine-grained acknowledgement control in your listener method.

## Manual Acknowledgement

You can set the `@RabbitListener` attribute `ackMode` to `MANUAL`, and customize the acknowledgment mode with listener method parameters: `AmqpAcknowledgment` and `Consumer.Context`.

```java
@Component
class AckListener {
    private static final Logger log = LoggerFactory.getLogger(AckListener.class);
    final List<String> received = Collections.synchronizedList(new ArrayList<>());

    CountDownLatch consumeIsDone = new CountDownLatch(11);

    @RabbitListener(queues = {HELLO_QUEUE_NAME},
              ackMode = "#{T(org.springframework.amqp.core.AcknowledgeMode).MANUAL}",
            concurrency = "2",
            id = "testAmqpListener")
    void processAckManually(String data, AmqpAcknowledgment acknowledgment, Consumer.Context context) {
        log.debug(":: received data: [{}]", data);
        try {
            if ("discard".equals(data)) {
                if (!this.received.contains(data)) {
                    log.debug(":: ack with discard");
                    context.discard();
                } else {
                    log.debug(":: throw new MessageConversionException");
                    throw new MessageConversionException("Test message is rejected");
                }
            } else if ("requeue".equals(data) && !this.received.contains(data)) {
                log.debug(":: ack with requeue");
                acknowledgment.acknowledge(AmqpAcknowledgment.Status.REQUEUE);
            } else {
                log.debug(":: ack with accept");
                acknowledgment.acknowledge();
            }
            this.received.add(data);
            log.debug(":: current received:{}", this.received);
        } finally {
            this.consumeIsDone.countDown();
        }
    }
}
```

When the message payload is `discard`, the message is discarded without requeueing; when the payload is `requeue`, the message is requeued for redelivery; otherwise the message is accepted.

You can write a test to verify the manual acknowledgment works as expected:

```java
@SpringJUnitConfig(classes = {
        AckListenerContainerTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class AckListenerContainerTest {
    private final Logger log = LoggerFactory.getLogger(AckListenerContainerTest.class);

    @Configuration
    @Import({
            RabbitClientConfig.class,
            AckListener.class
    })
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Autowired
    AckListener ackListener;

    @Test
    void testAck() {
        log.debug("Start sending message...");
        var initialFuture = CompletableFuture.completedFuture(true);
        var words = List.of(
                "the",
                "discard",
                "quick",
                "dog",
                "jumped",
                "over",
                "the",
                "requeue",
                "lazy",
                "discard",
                "fox");
        for (String word : words) {
            initialFuture = initialFuture
                    .thenComposeAsync(_ -> rabbitAmqpTemplate
                            .convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, word)
                            .whenComplete((res, ex) -> log.debug("sent: [{}]", word))
                    );
        }

        initialFuture.join();

        Awaitility.await().atMost(Duration.ofMillis(1_5000))
                .untilAsserted(() -> {
                    List<String> received = this.ackListener.received;
                    log.debug(">>> ackListener received: {}", received);

                    Map<String, Long> wordCount = received.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                    log.debug("word count: {}", wordCount);

                    assertThat(wordCount.get("discard")).isEqualTo(1);
                    assertThat(wordCount.get("the")).isEqualTo(2);
                });

    }

}
```

The test similarly sends a list of words to the queue, and verifies that the `discard` message is only received once, and the second `discard` message is rejected and caused an exception.

## Messaging Conversion

Finally, to use a POJO class as the message payload, you can configure a JSON message converter with Jackson to convert the message payload between JSON strings and type-safe objects.  

Add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    ...
    <dependency>
        <groupId>tools.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

Then declare a `JsonMapper` bean in your configuration class:

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

Then configure a `JacksonJsonMessageConverter` bean and set it to the `RabbitAmqpTemplate`:

```java
public class RabbitClientConfig {
    //...
    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    RabbitAmqpTemplate rabbitAmqpTemplate(AmqpConnectionFactory connectionFactory,
                                          MessageConverter jsonMessageConverter) {
        //...
        rabbitAmqpTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitAmqpTemplate;
    }
}
```    

Now create a simple `record` class to present the message payload:

```java
public record Greeting(String body, Instant sentAt) {
}
```

Create a test to verify sending and receiving JSON messages:

```java
@SpringJUnitConfig(classes = {
        RabbitAmqpTemplateTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class RabbitAmqpTemplateTest {

    @Configuration
    @Import({
            JacksonJsonMapperConfig.class,
            RabbitClientConfig.class
    })
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Test
    void testSendAndReceive_JSON() throws Exception {
        assertThat(this.rabbitAmqpTemplate.convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, new Greeting("test", Instant.now())))
                .succeedsWithin(Duration.ofSeconds(10));

        assertThat(this.rabbitAmqpTemplate.receiveAndConvert(HELLO_QUEUE_NAME, ParameterizedTypeReference.<Greeting>forType(Greeting.class)))
                .succeedsWithin(Duration.ofSeconds(10))
                .matches(it -> it.body().equals("test"));
    }
}
```

You can also create a listener to receive JSON messages:

```java
@Component
class GreetingListener {
    private static final Logger log = LoggerFactory.getLogger(GreetingListener.class);
    final List<Greeting> received = new ArrayList<>();

    @RabbitListener(queues = RabbitClientConfig.HELLO_QUEUE_NAME,
            concurrency = "2",
            id = "helloListener",
            messageConverter = "jsonMessageConverter"
    )
    void handleGreeting(/*Message data*/ Greeting greeting) {
        log.info("Converted message payload: {}", greeting);
        received.add(greeting);
    }
}
```

In the `@RabbitListener`, you have to set the `messageConverter` attribute to refer to the `JacksonJsonMessageConverter` bean. Then the payload will be converted to a `Greeting` object and available to be injected as method parameters automatically. Otherwise, you would have to use the generic `Message` object and extract the payload data manually.

!!! note
    Unlike other rabbit listener container, there is no `messageConverter` property in the `RabbitAmqpListenerContainerFactory` bean to apply the message conversion globally, see: https://github.com/spring-projects/spring-amqp/issues/3274

Create a test to verify the JSON listener works as expected:

```java
@SpringJUnitConfig(classes = {
        RabbitAmqpListenerContainerTest.TestConfig.class
})
@ContextConfiguration(initializers = {RabbitContainerInitializer.class})
public class RabbitAmqpListenerContainerTest {
    private final Logger log = LoggerFactory.getLogger(RabbitAmqpListenerContainerTest.class);

    @Configuration
    @Import({JacksonJsonMapperConfig.class,
            RabbitClientConfig.class,
            GreetingListener.class
    })
    static class TestConfig {
    }

    @Autowired
    RabbitAmqpTemplate rabbitAmqpTemplate;

    @Autowired
    GreetingListener greetingListener;

    @Test
    void testGreetingListener() {
        log.debug("Start sending message...");
        rabbitAmqpTemplate.convertAndSend(HELLO_EXCHANGE_NAME, HELLO_ROUTING_KEY, new Greeting("Hello", Instant.now()))
                .whenComplete((aBoolean, throwable) -> log.debug("Sending message is completed!!!"));

        Awaitility.await().atMost(Duration.ofMillis(1_000))
                .untilAsserted(() -> {
                    List<Greeting> received = greetingListener.received;
                    assertThat(received.size()).isEqualTo(1);
                    assertThat(received.getFirst().body()).isEqualTo("Hello");
                });

    }

}
```

Grab the example code from the [spring7-sandbox/rabbit-client](https://github.com/hantsy/spring7-sandbox/tree/master/rabbit-client) and [spring7-sandbox/rabbit-client-json](https://github.com/hantsy/spring7-sandbox/tree/master/rabbit-client-json) projects on GitHub.

## Spring Boot Integration

In the previous sections, you declared the client beans manually. Spring Boot 4.2 auto-configures them through two starters:

* `spring-boot-starter-amqp` builds on `spring-amqp-client`, the generic AMQP 1.0 client that works with any AMQP 1.0-compliant broker, for example ActiveMQ Artemis.
* `spring-boot-starter-amqp-rabbitmq` builds on `spring-rabbitmq-client`, the RabbitMQ-tailored client used in the previous sections.

### Generic AMQP 1.0 with ActiveMQ Artemis

ActiveMQ Artemis supports AMQP 1.0 natively, so it works with the generic client out of the box. To send and receive messages through an Artemis broker, add the following dependencies to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jackson</artifactId>
</dependency>
```

No Artemis-specific starter is required: `spring-boot-starter-artemis` provides JMS support, while the AMQP 1.0 client talks to the broker over the AMQP protocol directly.

The auto-configuration exposes an `AmqpClient` bean for sending messages and processes `@AmqpListener` annotations for consuming. To exchange JSON payloads, declare a `JacksonJsonMessageConverter` bean:

```java
@Bean
MessageConverter messageConverter() {
    return new JacksonJsonMessageConverter();
}
```

The message payload is a simple record:

```java
public record Greeting(String message, Instant sentAt) {
    public static Greeting of(String message) {
        return new Greeting(message, Instant.now());
    }
}
```

Declare a listener as follows:

```java
@Component
public class GreetingListener {
    private final static Logger log = LoggerFactory.getLogger(GreetingListener.class.getName());

    public List<String> messages = new ArrayList<>();

    @AmqpListener(addresses = "/queues/hello")
    public void onGreeting(Greeting greeting) {
        log.debug("received greeting: {} at {}", greeting, LocalDateTime.now());
        messages.add(greeting.message());
    }
}
```

The `@AmqpListener` annotation is from the package `org.springframework.amqp.client.annotation`. The `addresses` attribute uses the Artemis AMQP 1.0 address format `/queues/QUEUE_NAME`.

Configure the client in `application.properties`:

```properties
spring.amqp.client.default-to-address=/queues/hello
spring.amqp.client.completion-timeout=500ms
```

The `spring.amqp.client.default-to-address` property sets the destination used by `AmqpClient.sendToDefault`, and `spring.amqp.client.completion-timeout` bounds the asynchronous send operations. The connection itself is configured through `spring.amqp.host`, `spring.amqp.port`, `spring.amqp.username`, and `spring.amqp.password`.

To run against a real broker in tests, add the test dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-activemq</artifactId>
    <scope>test</scope>
</dependency>
```

The `spring-boot-amqp` module provides `@ServiceConnection` support for `ArtemisContainer`, so the container's host and port flow into the auto-configured client:

```java
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    ArtemisContainer artemisContainer() {
        return new ArtemisContainer("apache/activemq-artemis:2.30.0-alpine");
    }
}
```

Now create a test to verify sending and receiving messages:

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private AmqpClient amqpClient;

    @Autowired
    private GreetingListener listener;

    @Test
    public void testSendMessage() {
        amqpClient.to("/queues/hello")
                .body(Greeting.of("Hello AMQP"))
                .send()
                .join();

        waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("Hello AMQP"));
    }
}
```

The fluent `to(address).body(payload).send()` API targets an explicit address and converts the payload with the configured `MessageConverter`. The `send` method returns a `CompletableFuture`, so you can `join()` it to wait for the broker acknowledgement. To send to the configured default address instead, use `amqpClient.sendToDefault(message)`.

### RabbitMQ-tailored AMQP 1.0

If you are using RabbitMQ, the tailored starter wires the client beans for you, replacing the manual setup from [Getting Started](#getting-started). Add the following dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp-rabbitmq</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jackson</artifactId>
</dependency>
```

The starter auto-configures the `AmqpConnectionFactory`, `RabbitAmqpAdmin`, `RabbitAmqpTemplate`, and the listener container factory — the same beans declared manually in the plain Spring setup. You still declare the topology and the message converter as beans:

```java
@SpringBootApplication
public class DemoApplication {

    public final static String QUEUE_HELLO = "q.hello";
    public final static String EXCHANGE_HELLO = "x.hello";
    public final static String ROUTING_HELLO = "r.hello";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    Queue helloQueue() {
        return new Queue(QUEUE_HELLO, true);
    }

    @Bean
    DirectExchange helloExchange() {
        return new DirectExchange(EXCHANGE_HELLO);
    }

    @Bean
    Binding helloBinding() {
        return BindingBuilder.bind(helloQueue()).to(helloExchange()).with(ROUTING_HELLO);
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
```

The message payload is the same `Greeting` record as in [Generic AMQP 1.0 with ActiveMQ Artemis](#generic-amqp-10-with-activemq-artemis). The `JacksonJsonMessageConverter` bean is applied automatically: the listener method accepts the `Greeting` record directly, without setting the `messageConverter` attribute on `@RabbitListener`.

```java
@Component
@Slf4j
public class GreetingListener {

    public List<String> messages = new ArrayList<>();

    @RabbitListener(queues = {DemoApplication.QUEUE_HELLO})
    public void onGreeting(Greeting greeting) {
        log.debug("received greeting: {} at {}", greeting, LocalDateTime.now());
        messages.add(greeting.message());
    }
}
```

For the test, start a RabbitMQ container through `@ServiceConnection`:

```java
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"));
    }
}
```

!!! note
    The `rabbitmq:latest` image is RabbitMQ 4.x, which supports AMQP 1.0 by default. The test dependencies are `spring-boot-starter-amqp-rabbitmq-test`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, and `testcontainers-rabbitmq`.

Then send a JSON message through the auto-configured `RabbitAmqpTemplate` and verify the listener receives it:

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private RabbitAmqpTemplate amqpTemplate;

    @Autowired
    private GreetingListener listener;

    @Test
    public void testSendMessage() {
        amqpTemplate.convertAndSend(
                DemoApplication.EXCHANGE_HELLO,
                DemoApplication.ROUTING_HELLO,
                Greeting.of("Hello Rabbit")
        );

        waitAtMost(Duration.ofMillis(5_000))
                .untilAsserted(() -> assertThat(this.listener.messages).containsExactly("Hello Rabbit"));
    }
}
```

Grab the example code from the [spring7-sandbox/boot-amqp-artemis](https://github.com/hantsy/spring7-sandbox/tree/master/boot-amqp-artemis) and [spring7-sandbox/boot-amqp-rabbit](https://github.com/hantsy/spring7-sandbox/tree/master/boot-amqp-rabbit) projects on GitHub.
