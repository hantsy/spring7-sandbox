# AOT Build Improvements in Spring Data

In Spring 5 and Spring 6, you could use GraalVM build tools to compile applications into native executables and run them directly on your operating system or in a Docker container. There is no doubt that the native application startup times are impressive, but the build process can be slow and fragile for applications that use reflection, dynamic proxies, and runtime class loading.

With Spring 7 and Spring Boot v4, Spring Data modules such as JDBC, JPA, and MongoDB, etc. can now generate AOT metadata during compilation. That means the framework moves much of its runtime reflection work into build time, so the same application can target both JVM and native images without significant code changes.

For developers, you can continue writing Spring Data repositories and entities, and Spring will generate the metadata classes needed for both native images and JVM execution.

Let’s build a small example around a typical e-commerce model: `Customer`, `Order`, and `Product`.

```java
@Entity
@Table(name = "customers")
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class Customer {
    @EmbeddedId
    private CustomerId id;
    private String firstName;
    private String lastName;
    private String email;
}

@Embeddable
public record CustomerId(UUID id) {
    public CustomerId() {
        this(UUID.randomUUID());
    }
}

@Entity
@Table(name = "orders")
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    List<OrderItem> items;

    OrderStatus status = OrderStatus.PENDING;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "customer_id"))
    })
    CustomerId customerId;

    @CreatedDate
    Instant createdAt;
}

enum OrderStatus {
    PENDING(0),
    COMPLETED(1),
    DELIVERED(2),
    CANCELED(-1);

    @EnumeratedValue
    final int code;

    OrderStatus(int code) {
        this.code = code;
    }
}

@Embeddable
record OrderItem(String name, int quantity, @ManyToOne Product product) {
    OrderItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (product == null) {
            throw new IllegalArgumentException("Product must not be null");
        }
    }
}

@Entity
@Table(name = "products")
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    String name;
    BigDecimal price;
}
```

In this example, `Customer` uses an embedded key `CustomerId`, and `Order` contains a collection of `OrderItem` records. The `OrderStatus` enum is stored as an integer using `@EnumeratedValue`.

> [!Note]
> The record type embeddable classes like `CustomerId` and `OrderItem`, as well as the `@EnumeratedValue` support are [new features introduced in Jakarta Persistence 3.2](https://medium.com/itnext/an-introduction-to-jakarta-persistence-3-2-by-examples-69b34adc9c0b).

Create the Spring Data repositories as usual.

```java
public interface CustomerRepository extends JpaRepository<Customer, CustomerId> {
}

public interface OrderRepository extends JpaRepository<Order, Long> {
}

public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

Here’s an example of using these repositories to save a customer, products, and an order:

```java
var customerId = new CustomerId();
var customer = customerRepository.save(
        Customer.of(customerId, "Foo", "Bar", "foobar@example.com"));

var apple = productRepository.save(Product.of(null, "Apple", BigDecimal.ONE));
var orange = productRepository.save(Product.of(null, "Orange", BigDecimal.TEN));

var order = orderRepository.save(
        Order.of(customerId,
                List.of(
                        new OrderItem(apple.name, 10, apple),
                        new OrderItem(orange.name, 5, orange)
                )
        )
);
```

This code demonstrates a normal Spring Data JPA workflow, without any AOT-specific changes.

Spring Boot’s Maven plugin already provides AOT process support when you build projects with a built-in `native` Maven profile. 

```bash
mvn spring-boot:build-image -Pnative
```

If you want to extract AOT processing explicitly, add a custom Maven profile like this:

```xml
<profiles>
    <profile>
        <id>aot</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <executions>
                        <execution>
                            <phase>compile</phase>
                            <goals>
                                <goal>process-aot</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Then generate AOT metadata with:

```bash
mvn compile -Paot
```

After the build completes, you’ll find generated AOT metadata classes under `target/classes`.

For example, for the `CustomerRepository` interface, the compile task ( with the bound `process-aot` goal) may generate the following files, including:

- `CustomerRepository.json` — metadata describing repository methods
- `CustomerRepositoryImpl__AotRepository` — repository implementation using `EntityManager`/`CriteriaBuilder`
- `CustomerRepository__BeanDefinition` — programatic bean definition for repository registration

You can run the following command to generate AOT metadata files and package them in the final application `jar` file, then run the jar as regularly on the JVM:

```bash
mvn clean package -Paot
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

Or run with the Spring Boot plugin during development:

```bash
mvn spring-boot:run -Paot
```

If you want more details on the Spring Boot Maven plugin AOT support, check the official docs: [Ahead-of-Time Processing](https://docs.spring.io/spring-boot/maven-plugin/aot.html)

Check the example project on [GitHub](https://github.com/hantsy/spring7-sandbox/tree/master/boot-data-jpa).
