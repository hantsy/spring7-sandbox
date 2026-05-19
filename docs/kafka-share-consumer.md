# Kafka Share Consumer

Apache Kafka has long been the gold standard for high-throughput, low-latency data streaming. Historically, Kafka consumption relied on a "per-partition" model, where each partition in a topic was assigned to exactly one consumer within a group. 

Starting with Kafka 4.0 (via KIP-932), a new paradigm was introduced: **Share Consumer(Queues)**. This feature brings "per-message" semantics to the Kafka ecosystem. It is similar to traditional message queues, and allows multiple consumers in a "share group" to pull messages from the same topic simultaneously regardless of which partition it came from. While Kafka 4.0 introduced this as an experimental feature, it reached General Availability in Kafka 4.2.

Understanding the architectural shift is crucial for choosing the right tool for your use case:

| Feature | Traditional Consumer Group | Share Group |
| :--- | :--- | :--- |
| **Processing Granularity** | Per-partition (Exclusive access) | Per-message (Competing consumers) |
| **Concurrency Limit** | Limited by the number of partitions | Virtually unlimited; limited only by consumer resources |
| **Message Ordering** | Guaranteed within a single partition | No strict ordering guarantees across the group |
| **Delivery State** | Managed via offsets (Log-based) | Managed per message (Locking/Ack-based) |
| **Scalability** | To scale, you must add more partitions | You can add consumers without modifying the topic |
| **Use Case** | Event sourcing, stream processing, ordered logs | Work queues, competing consumers, slow task processing |

A Share Consumer group is ideal when you have a high volume of independent messages where the processing time might vary, or when you want to avoid "head-of-line blocking" where one slow message stalls an entire partition.

[Spring for Kafka 4.0 has added first-class Share Consumer support](https://spring.io/blog/2025/10/14/introducing-spring-kafka-share-consumer), providing the familiar `@KafkaListener` programming model for these new share groups.

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
    NewTopic myTopic() {
        return new NewTopic(DEMO_TOPIC_NAME, 1, (short) 1);
    }

    @Bean
    public ShareConsumerFactory<String, String> shareConsumerFactory() {
        log.debug("Get bootstrap servers from properties:{}", bootstrapServers);
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, DEMO_GROUP_NAME,
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
 
The configuration above defines a specialized `ShareConsumerFactory` along with a `ShareKafkaListenerContainerFactory` that utilizes it. We have also registered a lifecycle listener with the factory to monitor and log when consumers are dynamically added to or removed from the share group.

With the infrastructure in place, you can now define a `@KafkaListener` that leverages this container factory to participate in the share group:

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

Create a test to verify the functionality of the Share consumer against a running Kafka instance in testcontainers:

```java
@Testcontainers
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    // Kafka 4.2 enabled share consumer by default
    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:latest"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private GreetingListener listener;

    @Test
    public void testSendMessage() {
        List.of("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog")
                .forEach(word -> kafkaTemplate.send(DemoApplication.DEMO_TOPIC_NAME, word)
                        .thenAccept(s -> log.debug("sent message: {}", s)));

        Awaitility.waitAtMost(Duration.ofMillis(30_000))
                .untilAsserted(() -> assertThat(this.listener.getWordCount("the")).isEqualTo(2));
    }

}
```

Grab a copy of the example from [GitHub](https://github.com/hantsy/spring7-sandbox/tree/main/boot-kafka-share-consumer) and run the test to see the Share consumer in action. You should see the messages being consumed by the Share consumer and the word count being updated accordingly.
