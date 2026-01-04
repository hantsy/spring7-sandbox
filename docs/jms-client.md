# An Introduction to Spring JmsClient API

In previous posts, we discussed the new [`JdbcClient`](https://itnext.io/an-introduction-to-spring-jdbcclient-api-20e833d7b0f3) and [`RestClient`](https://medium.com/itnext/an-introduction-to-spring-restclient-api-22bfafcf9405) introduced in Spring 6 which used to replace the existing `JdbcTemplate` and `RestTemplate`. We are all impressed by these developer-friendly APIs. 
 
Spring 7 introduces [`JmsClient`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/core/JmsClient.html), a more modern and fluent API to interact with JMS(Jakarta Message Service) brokers, which is intended to replace the existing [`JmsTemplate`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/core/JmsTemplate.html) and [`JmsMessagingTemplate`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/core/JmsMessagingTemplate.html).

## Getting Started

Start by creating a Maven project and including the `spring-core`, `spring-context`, `spring-context-support` dependencies to provide the basic Spring container support. 

Add the following dependencies in your `pom.xml` file:

```xml 
// add spring core, context, context support dependencies if not already present
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jms</artifactId>
</dependency>

<dependency>
    <groupId>org.apache.activemq</groupId>
    <artifactId>artemis-jakarta-client</artifactId>
    <version>2.44.0</version>
</dependency>
```

Create a configuration class and declare a JMS `ConnectionFactory`, `MessageConverter` and `JmsListenerContainerFactory` bean to enable JMS support:

```java
@Configuration
@EnableJms
public class JmsConfig{

    @Autowired
    Environment environment;

    @Bean
    public CachingConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(
                new ActiveMQConnectionFactory(environment.getProperty("activemq.brokerUrl"), "user", "password")
        );
    }

    // The legacy messageconveter is from: org.springframework.jms.support.converter
    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter messageConverter = new JacksonJsonMessageConverter();
        messageConverter.setTypeIdPropertyName("_type");
        return messageConverter;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setMessageConverter(messageConverter());
        return factory;
    }

}
```

The `MessageConverter` bean here is used to convert between POJO objects and JMS messages.

Declare a `JmsTemplate` bean directly with the `ConnectionFactory` bean.

```java
@Bean
public JmsTemplate jmsTemplate() {
    JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory());
    jmsTemplate.setMessageConverter(messageConverter());
    return jmsTemplate;
}
```

With the `messageConverter` property set, you can use `jmsTemplate` to send a POJO object, as well as a text message.

Spring JMS also embraces the Spring Messaging abstraction, and it provides a `JmsMessagingTemplate` which is based on Spring Messaging API. 

Declare a `JmsMessagingTemplate` in the configuration class, and configure the `messageConverter` property with Spring Messaging specific `MessageConverter`.

```java 
// The spring messaging specific message converter is from org.springframework.messaging.
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
//...

@Bean
public MessageConverter messagingMessageConverter() {
    return new JacksonJsonMessageConverter();
}

@Bean
public JmsMessagingTemplate jmsMessagingTemplate() {
    JmsMessagingTemplate jmsMessagingTemplate = new JmsMessagingTemplate(connectionFactory());
    jmsMessagingTemplate.setMessageConverter(messagingMessageConverter());
    return jmsMessagingTemplate;
}
```

You can create `JmsClient` from the existing `ConnectionFactory` bean or `JmsOperations` (eg. `JmsTemplate`) bean.

```java
@Bean
public JmsClient jmsClient() {
    return JmsClient.create(jmsTemplate());
}
```

Alternatively, declare `JmsClient` with `JmsClient.Builder` to customize with a global `MessageConverter` and `MessagePostProcessor`.

```java
public JmsClient jmsClient() {
    return JmsClient.builder(jmsTemplate())
        .messageConverter(...)
        .messagePostProcessor(...)
        .build();
}
```

## Sending and Receiving Messages

To bootstrap an ActiveMQ Artimes server at runtime, add the following testcontainers dependencies in your `pom.xml` file:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-activemq</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>
```

Create a `ApplicationContextInitializer` to start the Artimes container before Spring context is initialized:

```java
class ArtemisContainerInitializer implements ApplicationContextInitializer<@NotNull ConfigurableApplicationContext> {
    private static final Logger log = LoggerFactory.getLogger(ArtemisContainerInitializer.class);
    final static String DOCKER_IMAGE_NAME = "apache/activemq-artemis:latest-alpine";
    final static Integer DEFAULT_EXPOSED_PORT = 61616;
    final ArtemisContainer container = new ArtemisContainer(DOCKER_IMAGE_NAME)
            .withUser("user")
            .withPassword("password")
            .withExposedPorts(DEFAULT_EXPOSED_PORT);


    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        container.start();
        applicationContext.addApplicationListener((e) -> {
            if (e instanceof ContextClosedEvent) {
                container.stop();
            }
        });

        var brokerUrlFormat = "tcp://%s:%d";
        var brokerUrl = brokerUrlFormat.formatted(container.getHost(), container.getFirstMappedPort());
        log.debug("connection url is {}", brokerUrl);

        applicationContext.getEnvironment()
                .getPropertySources()
                .addLast(
                        new MapPropertySource("activemqProps",
                                Map.of("activemq.brokerUrl", brokerUrl)
                        )
                );
    }
}
```

Create a test to verify sending and receiving messages with the classic `JmsTemplate`.

```java 
@SpringJUnitConfig(value = {JmsTemplateTest.TestConfig.class})
@ContextConfiguration(initializers = {ArtemisContainerInitializer.class})
public class JmsTemplateTest {
    private final static Logger log = LoggerFactory.getLogger(JmsTemplateTest.class);

    @Configuration
    @Import(value = {JmsConfig.class})
    static class TestConfig {
    }

    @Autowired
    JmsTemplate jmsTemplate;

    @Test
    public void testSendAndReceive() {
        jmsTemplate.convertAndSend("test", "hello");

        // wait to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> assertThat(jmsTemplate.receiveAndConvert("test")).isEqualTo("hello"));
    }

    @Test
    public void testSendAndReceive_GreetingObject() {
        jmsTemplate.convertAndSend("testObject", new Greeting("Hello", Instant.now()));

        // wait to verify.
        await().atMost(Duration.ofMillis(1_500))
                .untilAsserted(() -> {
                    Object receivedObject = jmsTemplate.receiveAndConvert("testObject");
                    log.debug("received object: {}", receivedObject);

                    var greetingObject = (Greeting) receivedObject;
                    assertThat(greetingObject.body()).isEqualTo("Hello");
                });

    }
}
```

Alternatively, you can send and receive messages with Spring Messaging specific `JmsMessagingTemplate`.

```java
@Autowired
JmsMessagingTemplate jmsMessagingTemplate;

@Test
public void testSendAndReceive() {
    jmsMessagingTemplate.convertAndSend("test", "hello");

    // wait to verify.
    await().atMost(Duration.ofMillis(1_500))
            .untilAsserted(() -> assertThat(jmsMessagingTemplate.receiveAndConvert("test", String.class)).isEqualTo("hello"));
}

@Test
public void testSendAndReceive_GreetingObject() {
    jmsMessagingTemplate.convertAndSend("testObject", new Greeting("Hello JmsClient!", Instant.now()));

    // wait one second to verify.
    await().atMost(Duration.ofMillis(1_500))
            .untilAsserted(() -> {
                var receivedMessage = jmsMessagingTemplate.receiveAndConvert("testObject", Greeting.class);
                assertThat(receivedMessage).isNotNull();
                log.info("Greeting messages received via JmsMessagingTemplate: {}", receivedMessage);
                assertThat(receivedMessage.body()).isEqualTo("Hello JmsClient!");
            });
}
```

As you see, the `jmsMessagingTemplate` improved payload type resolves when receiving messages.

The following is the example testing code using the new `JmsClient`:

```java
@Test
public void testSendAndReceive() {
    jmsClient.destination("test").send( "Hello");

    // wait to verify.
    await().atMost(Duration.ofMillis(1_500))
            .untilAsserted(() -> {
                Optional<String> received = jmsClient.destination("test").receive(String.class);
                assertThat(received.isPresent()).isTrue();

                log.debug("Received message: {}", received.get());
                assertThat(received.get()).isEqualTo("Hello");
            });
}

@Test
public void testSendAndReceive_GreetingObject() {
    jmsClient.destination("testObject")
            .withTimeToLive(2_000)
            .withReceiveTimeout(1_000)
            .withPriority(1)
            .withDeliveryDelay(100)
            .withDeliveryPersistent(false)
            .send(new Greeting("Hello JmsClient!", Instant.now()));

    // wait to verify.
    await().atMost(Duration.ofMillis(1_500))
            .untilAsserted(() -> {
                var received = jmsClient.destination("testObject")
                        .receive(Greeting.class);
                assertThat(received).isPresent();
                
                log.info("Greeting messages received: {}", received.get());
                assertThat(received.get().body()).isEqualTo("Hello JmsClient!");
            });
}
```

In the `JmsClient` example, the traditional method parameters are replaced with intuitive fluent methods. You can configure the destination properties using the `withXXX` methods before sending operations.

With `jmsListenerContainerFactory` bean, both `JmsTemplate` and `JmsMessagingTamplate` work well with the listener methods which are annotated with `@JmsListener`. The new `JmsClient` also works seamlessly with the listener methods.

For example, create a `GreetingListener` to consume messages with a payload type `Greeting`.

```java
@Component
public class GreetingListener {
    private final Logger log = LoggerFactory.getLogger(GreetingListener.class);

    public List<Greeting> received = new ArrayList<>();

    @JmsListener(destination = "greeting")
    public void onMessage(Greeting message) {
        log.debug("receiving body: {}", message);
        received.add(message);
    }
}
```

Use `jmsClient` to send a `Greeting` message to the destination `greeting`, then check the received messages in the `GreetingListener`.

```java
@Autowired
JmsClient jmsClient;

@Autowired
GreetingListener receiver;

@Test
public void testGreetingListener() {
    jmsClient.destination("greeting").send(new Greeting("Hello", Instant.now()));

    // wait to verify.
    await().atMost(Duration.ofMillis(1_500))
            .untilAsserted(() -> {
                List<Greeting> received = receiver.received;
                log.debug(">>> received: {}", received);

                assertThat(received.size()).isEqualTo(1);
                assertThat(received.getFirst().body()).isEqualTo("Hello");
            });
}
```

## Spring Boot

Generate your project skeleton from https://start.spring.io, add `Spring for Apache ActiveMQ Artemis` and `Testcontainers` in the *Dependencies*, it will include the following dependencies in your `pom.xml` to enable JMS autoconfiguration support with ActiveMQ Artemis. 

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-artemis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-artemis-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Additionally, it will add a Testcontainers configuration to start an Artemis server in Docker for development and test environments. The transitive dependency tree includes `ativemq-artemis-client` which is responsible for connecting to the running Artemis server.

Now you can inject `JmsTempalate`, `JmsMessagingTemplate` and `JmsClient` freely in your Spring components.

Add a `JmsTemplate` compatible `MessageConverter` to send and receive POJO messages. 

```java
// from org.springframework.jms.support.converter
@Bean
JacksonJsonMessageConverter jacksonMessageConverter() {
    JacksonJsonMessageConverter messageConverter = new JacksonJsonMessageConverter();
    messageConverter.setTypeIdPropertyName("_type");
    return messageConverter;
}
```

Do not forget to add `spring-boot-starter-jackson` and `spring-boot-starter-jackson-test` to the project dependencies to enable Jackson autoconfiguration.

Check the example code for demonstrating [JmsTemplate](https://github.com/hantsy/spring7-sandbox/tree/master/jms), [JmsMessagingTemplate](https://github.com/hantsy/spring7-sandbox/tree/master/jms-messaging), [JmsClient](https://github.com/hantsy/spring7-sandbox/tree/master/jms-client) and [all-in-one Spring Boot](https://github.com/hantsy/spring7-sandbox/tree/master/boot-jms) from my GitHub account.
