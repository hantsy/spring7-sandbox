# An Introduction to new Spring RabbitMQ Client

Spring AMQP 4.0 brings a new module `spring-rabbitmq-client` which is based on the new RabbitMQ official Java client `com.rabbitmq.client:amqp-client`, which is aligned with AMQP 1.0 protocol, and is also required to upgrade to RabbitMQ 4.0 when using it.

>[!NOTE]
> AMQP 0.9.1 and AMQP 1.0 are two different protocols, and RabbitMQ 3.x supports both protocols, but enabling AMQP 1.0 support requires installing an extra plugin `rabbitmq_amqp1_0`, RabbitMQ 4.0 switches to use AMQP 1.0 by default. The previous Spring RabbitMQ module `spring-rabbit` is still based on AMQP 0.9.1 protocol. Spring AMQP 4.1 will introduce new generic `spring-amqp-client` to implement AMQP 1.0 protocol, see: [spring-amqp#3271](https://github.com/spring-projects/spring-amqp/issues/3271)

## Getting Started

Create a new simple Maven project with the basic `spring-core` and `spring-context` as dependencies, or generate a Spring Boot project via https://start.spring.io as the previous post.

>[!NOTE]
> Till now the Spring Boot 4.0 does not contain a starter for Spring RabbitMQ Client, you need to add the dependency and configuration manually.

Then add the following dependencies in your `pom.xml` file:

```xml
<dependencyManagement>
    <dependencies>
        ...
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-amqp-bom</artifactId>
            <version>${spring-amqp.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    ...
    <dependency>
        <groupId>org.springframework.amqp</groupId>
        <artifactId>spring-rabbitmq-client</artifactId>
    </dependency>
</dependencies>
```

Next let's create a simple configuration class:

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

In the above configuration class, we create the enssential beans, eg. `AmqpConnectionFactory`, `RabbitAmqpAdmin`, `RabbitAmqpTemplate`, and `RabbitAmqpListenerContainerFactory` for Spring RabbitMQ client. Make sure the `Environment` and `AmqpEnvironmentBuilder` is from package `com.rabbitmq.client.amqp`.

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

To boostrap a RabbitMQ server at runtime, add the following testcontainers dependencies in your `pom.xml`:

```xml
 <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-rabbitmq</artifactId>
    <version>${testcontainers.version}</version>
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

Make sure you are using RabittMQ 4.x image to get AMQP 1.0 protocol support by default.

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

The `RabbitAmqpTemplate` imeplemnets `AsyncAmqpTemplate`, so all send and receive operations are asynchronous and return `CompletableFuture`.

The `RabbitAmqpTempalte` also supports RPC-style messaging via `convertSendAndReceive` method:

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

We have configured the listener container bean `RabbitAmqpListenerContainerFactory` in the configuration class, now you can use `@RabbitListener` to consume messages as usual:

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

The `HelloListener` component will receive messages sent to the `HELLO_QUEUE_NAME` queue, and collected in the `received` list.

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

# Messaging Conversion

Finally, to use JSON message converter with Jackson,  add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    ...
    <dependency>
        <groupId>tools.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

Then delcare a `JsonMapper` bean in your configuration class:

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

Now create a simple record class to present the message payload:

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

In the `@RabbitListener`, set the `messageConverter` attribute to refer to the `JacksonJsonMessageConverter` bean. Then the payload will be converted to a `Greeting` object and injected as method parameters automatically. Otherwise, you would have to use the generic `Message` object and extract the payload data manually.

> [!NOTE]
> Unlike other rabbit listener container, there is no global message converter property in the listener container factory, see: https://github.com/spring-projects/spring-amqp/issues/3274

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
