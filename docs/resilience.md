# Resilience Support in Spring 7

Spring 7 integrates the proven resilience patterns from the `spring-retry` project directly into the core framework, offering robust capabilities for managing transient failures and controlling concurrent executions. The resilience framework provides both declarative approaches using `@Retryable` and `@ConcurrencyLimit` annotations, as well as programmatic control through the flexible `RetryTemplate` API.

## Declarative Resilience Support

To enable Spring 7's declarative resilience features, start by annotating your configuration class with `@EnableResilientMethods`. This enables the framework to process and apply resilience annotations throughout your application:

```java
@EnableResilientMethods
@Configuration
public class ResilienceConfig {
}
```

With the resilience infrastructure in place, you can now annotate any method with `@Retryable` to define automatic retry behavior. Consider this example:

```java
@Component
public class RetryableExample {

    private final Logger log = LoggerFactory.getLogger(RetryableExample.class);

    private int retryCount = 0;

    @Retryable(value = {ExampleException.class},
            maxRetries = 5,
            delay = 1500,
            jitter = 50,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public void test() {
        retryCount++;
        log.info("RetryableExample test:{}", retryCount);
        throw new ExampleException();
    }

    public int count() {
        return this.retryCount;
    }
}
```

In this example, the `test()` method automatically retries up to 5 times before ultimately throwing the `ExampleException` to the caller. Each retry attempt includes a configurable delay (1500 milliseconds) and a random jitter (up to 50 milliseconds) to prevent thundering herd scenarios when multiple clients retry simultaneously.

Let's create a test class to verify this behavior: 

```java
@SpringJUnitConfig(classes = {RetryableExample.class, ResilienceConfig.class})
public class RetryableExampleTest {

    @Autowired
    RetryableExample retryableExample;

    @Test
    public void test() {
        try {
            retryableExample.test();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ExampleException.class);
        }
        Awaitility.await().atMost(10_000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(retryableExample.count()).isEqualTo(6));
    }
}
```

The test invokes the `test()` method and verifies that it eventually throws `ExampleException`. We use Awaitility to asynchronously wait until the retry count reaches 6 (the final invocation does not trigger retry attempt), ensuring the framework behaves as expected even in asynchronous scenarios.

Beyond retry logic, Spring 7 also provides the `@ConcurrencyLimit` annotation for controlling resource utilization. This annotation restricts the number of concurrent invocations of a method, acting as a built-in semaphore. Here's a practical example:

```java
@Component
public class ConcurrencyLimitExample {

    private final Logger log = LoggerFactory.getLogger(ConcurrencyLimitExample.class);

    private int count = 0;

    @ConcurrencyLimit(limit = 5)
    public void test(Integer i) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        count = i;
        log.info("RetryableExample test:{}", count);
    }

    public int count() {
        return this.count;
    }
}
```

This configuration limits the `test()` method to a maximum of 5 concurrent executions. When additional threads attempt to invoke the method while the limit is reached, they are queued and must wait until one of the currently executing threads completes. This prevents resource exhaustion and ensures controlled, predictable performance under load.

To thoroughly test the concurrency limiting behavior, we'll configure a `ThreadPoolTaskExecutor` that simulates concurrent demand:

```java
@Configuration
static class AsyncConfig {

    public AsyncConfig() {
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3); // Number of concurrent threads
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("MyParallelThread-");
        executor.initialize();
        return executor;
    }
}
```

Running the following concurrent test:

```java
@Test
public void test() {
    try {
        IntStream.range(0, 10)
                .forEach(i -> {
                    CompletableFuture.runAsync(() -> example.test(i), taskExecutor);
                });

    } catch (Exception e) {
    }
}
```    

You'll observe the concurrency limit in action through this console output: 

```bash 
2026-04-05 11:29:25,249 INFO  [MyParallelThread-1] c.e.d.ConcurrencyLimitExample: RetryableExample test:0, thread: MyParallelThread-1
2026-04-05 11:29:25,263 INFO  [MyParallelThread-2] c.e.d.ConcurrencyLimitExample: RetryableExample test:1, thread: MyParallelThread-2
2026-04-05 11:29:25,262 INFO  [MyParallelThread-3] c.e.d.ConcurrencyLimitExample: RetryableExample test:2, thread: MyParallelThread-3
2026-04-05 11:29:25,761 INFO  [MyParallelThread-1] c.e.d.ConcurrencyLimitExample: RetryableExample test:3, thread: MyParallelThread-1
2026-04-05 11:29:25,767 INFO  [MyParallelThread-2] c.e.d.ConcurrencyLimitExample: RetryableExample test:4, thread: MyParallelThread-2
2026-04-05 11:29:25,772 INFO  [MyParallelThread-3] c.e.d.ConcurrencyLimitExample: RetryableExample test:5, thread: MyParallelThread-3
2026-04-05 11:29:26,266 INFO  [MyParallelThread-1] c.e.d.ConcurrencyLimitExample: RetryableExample test:6, thread: MyParallelThread-1
2026-04-05 11:29:26,272 INFO  [MyParallelThread-2] c.e.d.ConcurrencyLimitExample: RetryableExample test:7, thread: MyParallelThread-2
2026-04-05 11:29:26,273 INFO  [MyParallelThread-3] c.e.d.ConcurrencyLimitExample: RetryableExample test:8, thread: MyParallelThread-3
2026-04-05 11:29:26,769 INFO  [MyParallelThread-1] c.e.d.ConcurrencyLimitExample: RetryableExample test:9, thread: MyParallelThread-1
```

For the complete working example, visit the [GitHub repository](https://github.com/hantsylabs/spring7-sandbox/tree/main/resilience).


## Programmatic Resilience with RetryTemplate

While declarative annotations provide a clean, convention-based approach, the `RetryTemplate` class offers fine-grained, programmatic control over retry behavior. This flexible API allows you to configure every aspect of the retry strategy: maximum attempts, delays, jitter, exception matching, and listener callbacks for monitoring. This is particularly useful for complex scenarios requiring dynamic configuration or custom retry logic. Here's a comprehensive example:

```java
@Configuration
public class RetryConfig {
    private final Logger log = LoggerFactory.getLogger(RetryListener.class);

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate(
                RetryPolicy.builder()
                        .maxRetries(5)
                        .delay(Duration.ofMillis(1500))
                        .jitter(Duration.ofMillis(50))
                        .includes(ExampleException.class)
                        .build()
        );
        retryTemplate.setRetryListener(new RetryListener() {
            @Override
            public void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable) {
                log.debug("beforeRetry: retryPolicy={}, retryable={}", retryPolicy.getClass().getSimpleName(), retryable.getClass().getSimpleName());
            }

            @Override
            public void onRetrySuccess(RetryPolicy retryPolicy, Retryable<?> retryable, @Nullable Object result) {
                log.debug("onRetrySuccess: retryPolicy={}, retryable={}, result={}", retryPolicy.getClass().getSimpleName(), retryable.getClass().getSimpleName(), result);
            }

            @Override
            public void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
                log.debug("onRetryFailure: retryPolicy={}, retryable={}, throwable={}", retryPolicy.getClass().getSimpleName(), retryable.getClass().getSimpleName(), throwable.getMessage());
            }

            @Override
            public void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
                log.debug("onRetryPolicyExhaustion: retryPolicy={}, retryable={}, throwable={}", retryPolicy.getClass().getSimpleName(), retryable.getClass().getSimpleName(), exception.getMessage());
            }

            @Override
            public void onRetryPolicyInterruption(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
                log.debug("onRetryPolicyInterruption: retryPolicy={}, retryable={}, throwable={}", retryPolicy.getClass().getSimpleName(), retryable.getClass().getSimpleName(), exception.getMessage());
            }
        });

        return retryTemplate;
    }
}
```

Next, we'll apply this retry template to execute operations with automatic error recovery:

```java
@Component
public class RetryExample {

    private final Logger log = LoggerFactory.getLogger(RetryExample.class);

    private int retryCount = 0;

    public void test() {
        retryCount++;
        log.info("RetryableExample test:{}", retryCount);
        throw new ExampleException();
    }

    public int count() {
        return this.retryCount;
    }
}
```

Finally, here's a test demonstrating the `RetryTemplate` in action:

```java
@SpringJUnitConfig(classes = {RetryExample.class, RetryConfig.class})
public class RetryExampleTest {
    private static final Logger log = LoggerFactory.getLogger(RetryExampleTest.class);

    @Autowired
    RetryTemplate retryTemplate;

    @Autowired
    RetryExample retryExample;

    @Test
    public void test() {
        try {
            retryTemplate.execute(new Retryable<>() {
                @Override
                public @Nullable Object execute() throws Throwable {
                    retryExample.test();
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("exception:", e);
            assertThat(e).cause().isInstanceOf(ExampleException.class);
        }

        Awaitility.await().atMost(10_000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(retryExample.count()).isEqualTo(6));
    }
}
```

Explore the complete implementation in the [GitHub repository](https://github.com/hantsylabs/spring7-sandbox/tree/main/retry).