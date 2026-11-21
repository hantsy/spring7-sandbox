# Kafka Share Consumer

Apache Kafka has long been the gold standard for high-throughput, low-latency data streaming. Historically, Kafka consumption used a `per-partition` model, where each topic partition was assigned to exactly one consumer in a group.

With Kafka 4.0 and KIP-932, Kafka introduced **Share Consumer(Queues)**. This new paradigm brings `per-message` semantics to Kafka and behaves more like a traditional message queue. A share group allows multiple consumers to pull messages from the same topic concurrently, regardless of the originating partition. The feature was experimental in Kafka 4.0 and reached General Availability in Kafka 4.2.

Understanding the architectural shift is crucial for choosing the right tool for your use case:

| Feature | Traditional Consumer Group | Share Group |
| :--- | :--- | :--- |
| **Processing Granularity** | Per-partition (Exclusive access) | Per-message (Competing consumers) |
| **Concurrency Limit** | Limited by the number of partitions | Virtually unlimited; limited only by consumer resources |
| **Message Ordering** | Guaranteed within a single partition | No strict ordering guarantees across the group |
| **Delivery State** | Managed via offsets (Log-based) | Managed per message (Locking/Ack-based) |
| **Scalability** | To scale, you must add more partitions | You can add consumers without modifying the topic |
| **Use Case** | Event sourcing, stream processing, ordered logs | Work queues, competing consumers, slow task processing |

A Share Consumer group is a good fit when you have a high volume of independent messages, variable processing time, or want to avoid head-of-line blocking where one slow message stalls an entire partition.

[Spring for Kafka 4.0 has added first-class Share Consumer support](https://spring.io/blog/2025/10/14/introducing-spring-kafka-share-consumer), providing the familiar `@KafkaListener` programming model for these share groups.

## Annotation-Driven Listeners

Generate a Spring Boot 4 project via [Spring Initializr](https://start.spring.io/) with these dependencies:

* Spring for Apache Kafka
* Testcontainers
* Lombok

Then define a `ShareConsumerFactory` and a dedicated `ShareKafkaListenerContainerFactory` as follows.

```java
@Configuration
@Slf4j
class ShareConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Bean
    NewTopic demoTopic() {
        return new NewTopic(DEMO_TOPIC_NAME, 1, (short) 1);
    }

    @Bean
    public ShareConsumerFactory<String, String> shareConsumerFactory() {
        log.debug("Get bootstrap servers from properties:{}", bootstrapServers);
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                // ConsumerConfig.GROUP_ID_CONFIG, DEMO_GROUP_NAME, // set in the consumer side
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
                //ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, "explicit"
        );
        DefaultShareConsumerFactory<String, String> factory = new DefaultShareConsumerFactory<>(props);
        factory.addListener(new ShareConsumerFactory.Listener<>() {
            @Override
            public void consumerAdded(String id, ShareConsumer<String, String> consumer) {
                log.debug("consumer added id:{}", id);
            }

            @Override
            public void consumerRemoved(@Nullable String id, ShareConsumer<String, String> consumer) {
                log.debug("consumer removed id:{}", id);
            }
        });
        return factory;
    }

    @Bean
    public ShareKafkaListenerContainerFactory<String, String> shareKafkaListenerContainerFactory(
            ShareConsumerFactory<String, String> shareConsumerFactory) {
        return new ShareKafkaListenerContainerFactory<>(shareConsumerFactory);
    }
}
```
 
This configuration defines a specialized `ShareConsumerFactory` and a `ShareKafkaListenerContainerFactory` that uses it. It also registers a lifecycle listener to log when consumers join or leave the share group. The `NewTopic` bean lets `KafkaAdmin` create the demo topic on startup when you run the application outside the test.

With the infrastructure in place, define a `@KafkaListener` that uses this container factory to participate in the share group:

```java
@Component
@Slf4j
public class GreetingListener {
    public Map<String, Long> counter = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = DEMO_TOPIC_NAME,
            containerFactory = "shareKafkaListenerContainerFactory",
            groupId = DEMO_GROUP_NAME
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.debug("received record: {} at {}", record, LocalDateTime.now());
        counter.compute(record.value(), (s, v) -> v == null ? 1 : v + 1);
    }

    public Long getWordCount(String word) {
        return this.counter.get(word);
    }
}
```

Create a test to verify the share consumer behavior against a running Kafka instance in Testcontainers:

> [!NOTE]
> The broker image is pinned to `apache/kafka:4.4.0`, the very Kafka release that Spring Boot manages for the client side (the `kafka.version` property). Keep the broker and the client on the same Kafka release, otherwise the Share Consumer protocol may fail to be negotiated. The `KAFKA_SHARE_COORDINATOR_STATE_TOPIC_*` settings adapt the internal `__share_group_state` topic to a single broker, the default of Kafka is 3 replicas and a minimum ISR of 2. The topic is created before the application starts, so that the share group is formed with a non empty assignment.

```java
@Testcontainers
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    // Kafka 4.2 enabled share consumer by default
    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:4.4.0"))
        .withEnv("KAFKA_SHARE_COORDINATOR_STATE_TOPIC_REPLICATION_FACTOR", "1")
        .withEnv("KAFKA_SHARE_COORDINATOR_STATE_TOPIC_MIN_ISR", "1");

    @BeforeAll
    static void createTopic() throws Exception {
        // The share group has to know the topic before it is formed, otherwise its first
        // assignment is empty and the records produced in the meantime are skipped.
        try (var admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()))) {
            try {
                admin.createTopics(List.of(new NewTopic(DEMO_TOPIC_NAME, 1, (short) 1)))
                        .all().get(30, TimeUnit.SECONDS);
            }
            catch (ExecutionException ex) {
                if (!(ex.getCause() instanceof TopicExistsException)) {
                    throw ex;
                }
            }
        }
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private GreetingListener listener;

    private static void awaitShareGroupAssignment() {
        try (var admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()))) {
            Awaitility.waitAtMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        Map<String, ShareGroupDescription> groups;
                        try {
                            groups = admin.describeShareGroups(List.of(DEMO_GROUP_NAME))
                                    .all().get(10, TimeUnit.SECONDS);
                        }
                        catch (ExecutionException ex) {
                            // The group does not exist until the first consumer joins; keep polling.
                            if (ex.getCause() instanceof GroupIdNotFoundException) {
                                throw new AssertionError("The share group is not available yet", ex);
                            }
                            throw new IllegalStateException("Failed to describe the share group", ex);
                        }
                        catch (TimeoutException ex) {
                            throw new AssertionError("Timed out describing the share group", ex);
                        }
                        catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted while describing the share group", ex);
                        }
                        var group = groups.get(DEMO_GROUP_NAME);
                        assertThat(group).isNotNull();
                        assertThat(group.members())
                                .anyMatch(member -> member.assignment().topicPartitions().stream()
                                        .anyMatch(tp -> tp.topic().equals(DEMO_TOPIC_NAME)));
                    });
        }
    }

    @Test
    public void testSendMessage() {
        awaitShareGroupAssignment();
        var sends = List.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog").stream()
                .map(word -> kafkaTemplate.send(DemoApplication.DEMO_TOPIC_NAME, UUID.randomUUID().toString(), word)
                        .thenAccept(s -> log.debug("sent message: {}", s)))
                .toList();
        CompletableFuture.allOf(sends.toArray(new CompletableFuture<?>[0])).join();

        Awaitility.waitAtMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(this.listener.getWordCount("the")).isEqualTo(2));
    }

}
```

>[!NOTE]
>I encountered some issues when running the test, so I added the `KAFKA_SHARE_COORDINATOR_STATE_TOPIC_REPLICATION_FACTOR` environment variable, created the topic before the application starts, and waited for the share group to receive a non-empty assignment before producing records. Check the original discussion on StackOverflow for more details: [Kafka Share Consumer issue](https://stackoverflow.com/questions/79943319/kafka-share-consumer-issue).

Grab the example code from [GitHub](https://github.com/hantsy/spring7-sandbox/tree/main/boot-kafka-share-consumer), then run the test to verify the share consumer is working and the word count updates correctly.

## Programmatic Listeners

Like the traditional consumer model, you can create programmatic listeners with `ShareKafkaMessageListenerContainer`. This gives finer control over consumer lifecycle and message processing.

```java
@Configuration
@Slf4j
class ShareConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Bean
    public ShareConsumerFactory<String, String> shareConsumerFactory() {
        log.debug("Get bootstrap servers from properties:{}", bootstrapServers);
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                // ConsumerConfig.GROUP_ID_CONFIG, DEMO_GROUP_NAME, // set in the consumer side
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );
        DefaultShareConsumerFactory<String, String> factory = new DefaultShareConsumerFactory<>(props);
        factory.addListener(new ShareConsumerFactory.Listener<>() {
            @Override
            public void consumerAdded(String id, ShareConsumer<String, String> consumer) {
                log.debug("consumer added id:{}", id);
            }

            @Override
            public void consumerRemoved(@Nullable String id, ShareConsumer<String, String> consumer) {
                log.debug("consumer removed id:{}", id);
            }
        });
        return factory;
    }

    @Bean
    public ShareKafkaMessageListenerContainer<String, String> shareKafkaMessageListenerContainer(
            ShareConsumerFactory<String, String> shareConsumerFactory, GreetingListener greetingListener) {

        ContainerProperties containerProps = new ContainerProperties(DEMO_TOPIC_NAME);
        containerProps.setGroupId(DEMO_GROUP_NAME);

        ShareKafkaMessageListenerContainer<String, String> container =
                new ShareKafkaMessageListenerContainer<>(shareConsumerFactory, containerProps);

        container.setupMessageListener(greetingListener);

        // container.setConcurrency(10);
        return container;
    }

}
```

See [the example project](https://github.com/hantsy/spring7-sandbox/blob/master/boot-kafka-share-consumer-prog-container) on GitHub for the full implementation, then inspect the test and `GreetingListener` class to confirm the programmatic listener behavior.

## Explicit Acknowledgement

By default, a Share consumer uses implicit acknowledgement: messages are considered acknowledged as soon as they are delivered. To control acknowledgement timing, enable explicit acknowledgement mode.

To enable explicit acknowledgement, set the `ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG` property to `explicit` in the consumer configuration, and switch the container to `ShareAckMode.MANUAL` so it delegates acknowledgement to the listener instead of acknowledging on delivery:

```java
@Configuration
@Slf4j
class ShareConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Bean
    public ShareConsumerFactory<String, String> explicitShareConsumerFactory() {
        Map<String, Object> props = Map.of(
                ...
                ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, "explicit"
        );
        return new DefaultShareConsumerFactory<>(props);
    }

    @Bean
    public ShareKafkaListenerContainerFactory<String, String> explicitShareKafkaListenerContainerFactory(
            ShareConsumerFactory<String, String> explicitShareConsumerFactory) {
        var factory = new ShareKafkaListenerContainerFactory<>(explicitShareConsumerFactory);
        // The listener acknowledges the records itself, so the container must run in MANUAL ack mode.
        factory.getContainerProperties().setShareAckMode(ContainerProperties.ShareAckMode.MANUAL);
        return factory;
    }
    ...

}
```

On the consumer side, you can then use the `ShareAcknowledgment` object to acknowledge messages explicitly:

```java
@KafkaListener(
        topics = DEMO_TOPIC_NAME,
        containerFactory = "explicitShareKafkaListenerContainerFactory",
        groupId = DEMO_GROUP_NAME
)
public void onMessage(ConsumerRecord<String, String> record, ShareAcknowledgment ack) {
    log.debug("received record: {} at {}", record, LocalDateTime.now());
    // Acknowledge outside the map update: ack/reject performs network I/O.
    if (counter.putIfAbsent(record.value(), 1L) == null) {
        ack.acknowledge();
    } else {
        ack.reject(); // reject when the word is already tapped.
    }
}
```

In this example, the consumer acknowledges a message only on first occurrence. If the word has already been processed, the listener rejects it and the broker will not retry it.

This explicit acknowledgement mode allows you to implement more complex processing logic and error handling strategies, giving you greater control over the message processing lifecycle in a Share consumer group.

Check the example project from [GitHub](https://github.com/hantsy/spring7-sandbox/blob/master/boot-kafka-share-consumer-explicit-ack) and explore the test code. You can run the test to see how `explicit` acknowledgement works in the Share consumer model.
