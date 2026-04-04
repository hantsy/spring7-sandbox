# Modularizing Spring Boot 4 Application

One breaking change in Spring Boot 4 is that the single monolithic `spring-boot-autoconfigure` module is split into smaller, fine-grained feature and library specific modules. Each feature or library now provides its own `starter` and `starter-test` modules, which allows developers to include only the autoconfiguration classes and third-party libraries their application requires.

## Transforming to Spring Boot 4

Go to [Spring Initializr](https://start.spring.io/), generate one Spring Boot 3 project and one Spring Boot 4 project using the dependencies `Web`, `Data JPA`, and `Security`, and then compare the generated `pom.xml` files to see how the dependencies differ.

The Spring Boot 3 example below uses a single shared test starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
</dependency>
```

The Spring Boot 4 example shows that `spring-boot-starter-web` is now `spring-boot-starter-webmvc`, and each feature module now has its own test starter.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

In Spring Boot 4, the autoconfiguration classes that used to be bundled in the large `spring-boot-autoconfigure` module are now distributed across specific feature modules. In the example above, `spring-boot-starter-security-test` pulls in `spring-security-test` plus the matching autoconfiguration classes. The package structure has also been reorganized by feature (e.g. `...autoconfigure.data.jpa` becomes `...data.jpa.autoconfigure`).

For a complete list of Spring Boot 4 modules, see the [Module dependencies section of the Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#module-dependencies).

If you only need a few smaller features (for example, Jackson, Flyway, or Liquibase), each of them now has dedicated `starter` and `starter-test` modules. For Jackson, add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jackson</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jackson-test</artifactId>
    <scope>test</scope>
</dependency>
```

We have discussed the [new Jackson 3 support in Spring 7 and Spring Boot 4](./jackson.md).

Spring Boot 4 also modularizes Spring Data dependencies by moving database drivers and client SDKs into dedicated modules. For example:

- JDBC `JdbcClient` and R2DBC `DatabaseClient` are now exposed by `spring-boot-starter-jdbc` and `spring-boot-starter-r2dbc`.
- MongoDB Java client support (synchronous or reactive `MongoClient`) is exposed by `spring-boot-starter-mongodb`.

This makes it possible to depend on the specific client libraries you need without pulling in full Spring Data starter sets.

Let's explore the Spring Data client support in Spring Boot 4 in more detail in the next section.

## Spring Data Client Support

Let's start with the simplest JDBC `JdbcClient` and R2DBC `DatabaseClient` support, although I have described them in my previous articles.

### JDBC `JdbcClient`

Create a new Spring Boot 4 project via [Spring Initializr](https://start.spring.io/) with dependencies `JDBC API`, `PostgreSQL`, `Lombok` and `Testcontainers`.  The `spring-boot-starter-jdbc` starter will autoconfigure a `JdbcTemplate`, `NamedParameterJdbcTemplate`, and `JdbcClient` for you.

Create a simple record type `Post` to represent the row data in the database. 

```java
public record Post(UUID id,
                   String title,
                   String content,
                   Status status,
                   LocalDateTime createdAt
) {
    public static Post of(String title, String content) {
        return new Post(null, title, content, Status.DRAFT, null);
    }

    public static Post of(String title, String content, Status status) {
        return new Post(null, title, content, status, null);
    }
}

public enum Status {
    DRAFT, PENDING_MODERATION, PUBLISHED;
}
```

Declear a `PostRepository` interface to define the data access methods.

```javapublic interface PostRepository {
    Post save(Post post);
    Post findById(UUID id);
    List<Post> findAll();
    Integer update(Post post);
    Integer deleteById(UUID id);
    Integer deleteAll();
    // other methods are ommitted for brevity
}
```

Then implement the `PostRepository` interface using `JdbcClient`.

```java
@RequiredArgsConstructor
@Slf4j
@Repository
@Transactional
public class JdbcClientPostRepository implements PostRepository {

    public static final RowMapper<Post> ROW_MAPPER = (rs, rowNum) -> new Post(
            rs.getObject("id", UUID.class),
            rs.getString("title"),
            rs.getString("content"),
            //see: https://github.com/pgjdbc/pgjdbc/issues/2387
            //rs.getObject("status", Status.class),
            Status.valueOf(rs.getString("status")),
            rs.getObject("created_at", LocalDateTime.class)
    );

    private final JdbcClient client;


    @Override
    public List<Post> findAll() {
        var sql = "SELECT * FROM posts";
        return this.client.sql(sql)
                // added in Spring 7.0
                .withFetchSize(10)
                .withMaxRows(50)
                .withQueryTimeout(1_000)
                .query(ROW_MAPPER).list();
    }

    @Override
    public Post findById(UUID id) {
        var sql = "SELECT * FROM posts WHERE id=:id";
        return this.client.sql(sql).params(Map.of("id", id)).query(ROW_MAPPER).single();
    }

    @Override
    public UUID save(Post p) {
        var sql = """
                INSERT INTO  posts (title, content, status) 
                VALUES (:title, :content, :status)
                RETURNING id
                """;
        var keyHolder = new GeneratedKeyHolder();
        var paramSource = new MapSqlParameterSource(
                Map.of("title", p.title(), "content", p.content(), "status", p.status().name())
        );
        var cnt = this.client.sql(sql).paramSource(paramSource).update(keyHolder);
        log.debug("updated count:{}", cnt);
        return keyHolder.getKeyAs(UUID.class);
    }

    @Override
    public Integer update(Post p) {
        var sql = "UPDATE posts set title=:title, content=:content, status=:status WHERE id=:id";
        Map<String, ? extends Serializable> params = Map.of(
                "title", p.title(),
                "content", p.content(),
                "status", p.status().name(),
                "id", p.id()

        );
        return this.client.sql(sql).params(params).update();
    }

    @Override
    public Integer deleteById(UUID id) {
        var sql = "DELETE FROM posts WHERE id=:id";
        return this.client.sql(sql).params(Map.of("id", id)).update();
    }

}
```

Add a `schema.sql` file and `data.sql` in `src/main/resources` to initialize the database schema and data.

```sql
-- schema.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- create posts table
CREATE TABLE IF NOT EXISTS posts (
 id UUID DEFAULT uuid_generate_v4(),
 title VARCHAR(255),
 content VARCHAR(255),
 status VARCHAR(20) DEFAULT 'DRAFT',
 created_at TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP,
 PRIMARY KEY (id)
 );

-- data.sql
INSERT INTO  posts (title, content) VALUES ('Spring 6 and Jdbc', 'Review the Jdbc features in Spring framework 6.0');
```

Create a test class to verify the `PostRepository` implementation against a real PostgreSQL database running in Testcontainers.

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    @Autowired
    PostRepository posts;

    @BeforeEach
    public void setup() {
        var deleted = this.posts.deleteAll();
        log.debug("deleted posts: {}", deleted);
    }

    @Test
    public void testSaveAll() {
        var data = List.of(
                Post.of("test", "content", Status.PENDING_MODERATION),
                Post.of("test2", "content2"),
                Post.of("test3", "content3")
        );

        data.forEach(post -> {
            var saved = posts.save(post);
            log.debug("saved post: {}", saved);
        });

        Long count = posts.count();
        assertThat(count).isEqualTo(3L);

        var countByStatus = posts.countByStatus();
        log.debug("count by status: {}", countByStatus);
        assertThat(countByStatus.get(Status.DRAFT)).isEqualTo(2L);
    }

    @Test
    public void testInsertAndQuery() {
        var id = this.posts.save(Post.of("test title", "test content"));
        var saved = this.posts.findById(id);
        assertThat(saved.status()).isEqualTo(Status.DRAFT);

        var updatedCnt = this.posts.update(new Post(saved.id(), "updated test", "updated content", Status.PENDING_MODERATION, saved.createdAt()));
        assertThat(updatedCnt).isEqualTo(1);
        var updated = this.posts.findById(id);
        assertThat(updated.status()).isEqualTo(Status.PENDING_MODERATION);
    }

}
```

The full example code also include a `DataInitilizer` to add some data via observing `ApplicationReadyEvent`, check it on [GitHub](https://github.com/hantsy/spring7-sandbox/tree/master/boot-jdbc).

### R2DBC `DatabaseClient`

Similarly create a Spring Boot 4 project with `R2DBC API`, `PostgreSQL`, `Lombok` and `Testcontainers`. The `spring-boot-starter-r2dbc` starter will autoconfigure a `DatabaseClient` for you. Spring uses Reactor to handle reactive streams, so the repository methods will return `Mono` or `Flux` instead of direct values.

The `Post` record and `Status` enum are the same as the JDBC example above, so we can reuse them here.

The `PostRepository` interface is also similar, but the return types are now reactive types.

```java
public interface PostRepository {
    Flux<Post> findByTitleContains(String name);
    Mono<Post> save(Post post);
    Mono<Post> findById(UUID id);
    Flux<Post> findAll();
    Mono<Integer> update(Post post);
    Mono<Integer> deleteById(UUID id);
    Mono<Integer> deleteAll();
    // other methods are ommitted for brevity
}
```

Then implement the `PostRepository` interface using `DatabaseClient`.

```java
@RequiredArgsConstructor
@Component
@Slf4j
public class R2dbcClientPostRepository implements PostRepository {

    public static final BiFunction<Row, RowMetadata, Post> MAPPING_FUNCTION = (row, rowMetaData) -> new Post(
            row.get("id", UUID.class),
            row.get("title", String.class),
            row.get("content", String.class),
            Status.valueOf(row.get("status", String.class)),
            row.get("created_at", LocalDateTime.class)
    );

    private final DatabaseClient databaseClient;

    @Override
    public Flux<Post> findByTitleContains(String name) {
        String sql = """
                SELECT * FROM posts 
                         WHERE title LIKE :title
                """;
        return this.databaseClient
                .sql(sql)
                .bind("title", "%" + name + "%")
                .map(MAPPING_FUNCTION)
                .all();
    }

    @Override
    public Flux<Post> findAll() {
        String sql = """
                SELECT * FROM posts
                """;
        return this.databaseClient
                .sql(sql)
                .filter((statement, executeFunction) -> statement.fetchSize(10).execute())
                .map(MAPPING_FUNCTION)
                .all();
    }

    @Override
    public Mono<Post> findById(UUID id) {
        String sql = """
                SELECT * FROM posts 
                         WHERE id=:id
                """;
        return this.databaseClient
                .sql(sql)
                .bind("id", id)
                .map(MAPPING_FUNCTION)
                .one();
    }

    @Override
    public Mono<UUID> save(Post p) {
        String sql = """
                INSERT INTO  posts (title, content, status) 
                VALUES (:title, :content, :status)
                """;
        return this.databaseClient.sql(sql)
                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .bind("title", p.title())
                .bind("content", p.content())
                .bind("status", p.status().name())
                .fetch()
                .first()
                .map(r -> (UUID) r.get("id"));
    }

    // see: https://github.com/spring-projects/spring-data-r2dbc/issues/259
    // and
    // https://stackoverflow.com/questions/62514094/how-to-execute-multiple-inserts-in-batch-in-r2dbc
    @Override
    public Flux<UUID> saveAll(List<Post> data) {
        Assert.notEmpty(data, "saving data can be empty");
        return this.databaseClient.inConnectionMany(connection -> {

            String sql = """
                    INSERT INTO posts (title, content, status) 
                    VALUES ($1, $2, $3)
                    """;
            var statement = connection
                    .createStatement(sql)
                    .returnGeneratedValues("id");

            for (int i = 0; i < data.size() - 1; i++) {
                Post p = data.get(i);
                statement.bind(0, p.title())
                        .bind(1, p.content())
                        .bind(2, p.status().name())
                        .add();
            }

            // for the last item, do not call `add`
            var lastItem = data.get(data.size() - 1);
            statement.bind(0, lastItem.title())
                    .bind(1, lastItem.content())
                    .bind(2, lastItem.status().name());

            return Flux.from(statement.execute())
                    .flatMap(result -> result.map((row, rowMetadata) -> row.get("id", UUID.class)));
        });
    }

    @Override
    public Mono<Long> update(Post p) {
        String sql = """
                UPDATE posts 
                SET title=:title,
                    content=:content,
                    status=:status 
                WHERE id=:id
                """;
        return this.databaseClient
                .sql(sql)
                .bind("title", p.title())
                .bind("content", p.content())
                .bind("status", p.status().name())
                .bind("id", p.id())
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Long> deleteById(UUID id) {
        String sql = """
                DELETE FROM posts 
                       WHERE id=:id
                """;
        return this.databaseClient.sql(sql)
                .bind("id", id)
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Long> deleteAllById(List<UUID> ids) {
        String sql = """
                DELETE FROM posts 
                       WHERE id in (:ids)
                """;
        return this.databaseClient.sql(sql)
                .bind("ids", ids)
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Long> deleteAll() {
        String sql = """
                DELETE FROM posts
                """;
        return this.databaseClient.sql(sql)
                .fetch()
                .rowsUpdated();
    }
}
```

The test code is similar, we ignore it here for brevity, check it on [GitHub](https://github.com/hantsy/spring7-sandbox/tree/master/boot-r2dbc). One thing to note is that the Postgres testcontainer depends on Jdbc driver, so in the `pom.xml` you will see we add both Postgres R2dbc and Postgres JDBC dependencies.

### MongoDB `MongoClient`

Create a new Spring Boot 4 project with dependencies: `MongoDB`, `Lombok` and `Testcontainers`. Open the `pom.xml` file and add `org.testcontainters:testcontainers-mongodb` dependency to use MongoDB Testcontainers. The `spring-boot-starter-mongodb` starter will autoconfigure a `MongoClient` for you.

Create a simple POJO class `Product` to repsent the document data in MongoDB.

```java
@BsonDiscriminator("products")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Product {
    @BsonId @BsonRepresentation(BsonType.OBJECT_ID)
    String id;
    String name;
    BigDecimal price;

    Product withId(String newId) {
        return new Product(newId, this.name, this.price);
    }
}
```

> [WARNING!]
> At the moment I prepared the example code, the MongoClient API did not support a record type.

Create a `ProductRepository` class  to use `MongoClient` to operate the documents.

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {

    private final MongoClient mongoClient;
    private MongoCollection<Product> productsCollection;

    @PostConstruct
    public void init() {
        this.productsCollection = mongoClient
                .getDatabase("test")
                .getCollection("products", Product.class);
    }

    Product save(Product product) {
        var result = this.productsCollection.insertOne(product);
        log.debug("save product result: {}", result);
        var id = result.getInsertedId().asObjectId().getValue().toHexString();
        return product.withId(id);
    }

    Optional<Product> findById(String id) {
        var byId = this.productsCollection
                .find(Filters.eq(new ObjectId(id)))
                .first();
        log.debug("find product by id: {}", byId);
        return Optional.ofNullable(byId);
    }

}
```

Add a test class to verify the `ProductRepository` implementation against a real MongoDB database running in Testcontainers.

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void testProductRepository() {
        var product = productRepository.save(new Product(null, "test", BigDecimal.ONE));
        assertThat(product).isNotNull();
        assertThat(product.getId()).isNotNull();

        Optional<Product> byId = productRepository.findById(product.getId());
        assertThat(byId).isPresent();

        var p = byId.get();
        log.debug("found product by id: {}", p);
        assertThat(p.getName()).isEqualTo("test");
    }
}
```

Check the full example code on [GitHub](https://github.com/hantsy/spring7-sandbox/tree/master/boot-r2dbc). One thing to note is that currently Spring Initializr does not generate a `TestcontainersConfiguration` class for MongoDB when adding `MongoDB`, so you need to create it yourself, or copy it from the generated result when add `Spring Data MongoDB` dependency into projects. 

### Neo4j `Driver`

Create a new Spring Boot 4 project with dependencies: `Neo4j`, `Lombok` and `Testcontainers`. Open the `pom.xml` file and add `org.testcontainers:testcontainers-neo4j` dependency to use Neo4j Testcontainers. The `spring-boot-starter-neo4j` starter will autoconfigure a Neo4j `Driver` for you.

Create a simple record type `Product` to represent the node data in Neo4j.

```java
public record Product(String id, String name, BigDecimal price) {
}
```

Create a `ProductRepository` class  to use `Driver` to operate the nodes.

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRepository {
    private final Driver driver;

    private Session session;

    @PostConstruct
    public void init() {
        this.session = driver.session();
    }

    Product save(Product product) {
        String query = """
                MERGE (p:Product {id: $id})
                ON CREATE SET p.name=$name, p.price=$price
                ON MATCH SET p.name=$name, p.price=$price
                RETURN p.id as id, p.name as name, p.price as price
                """;
        var result = this.session
                .executeWrite(tc -> tc.run(query,
                                        Map.of("id", product.id() != null ? product.id() : UUID.randomUUID().toString(),
                                                "name", product.name(),
                                                "price", Values.value(product.price().toString())
                                        )
                                )
                                .single()
                );

        log.debug("saving product {}", result);
        return new Product(result.get("id").asString(), result.get("name").asString(), new BigDecimal(result.get("price").asString()));
    }

    Optional<Product> findById(String id) {
        String query = """
                MATCH (p:Product)
                WHERE p.id=$id
                RETURN p.id as id, p.name as name, p.price as price
                """;
        var optionalRecord = this.session
                .executeRead(tc -> tc.run(query, Map.of("id", id))
                        .stream()
                        .findFirst()
                );
        log.debug("find product by id {}", optionalRecord);
        return optionalRecord
                .map(result -> new Product(result.get("id").asString(), result.get("name").asString(), new BigDecimal(result.get("price").asString())));

    }
}
```

Add a test class to verify the `ProductRepository` implementation against a real Neo4j database running in Testcontainers.

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Slf4j
class DemoApplicationTests {

 @Autowired
 private ProductRepository productRepository;

 @Test
 public void testProductRepository() {
  var product = productRepository.save(new Product(null, "test", BigDecimal.ONE));
  assertThat(product).isNotNull();
  assertThat(product.id()).isNotNull();

  Optional<Product> byId = productRepository.findById(product.id());
  assertThat(byId).isPresent();
  var p = byId.get();

  log.debug("found product by id: {}", p);
  assertThat(p.name()).isEqualTo("test");
 }
}
```

Check the full example code on [GitHub](https://github.com/hantsy/spring7-sandbox/tree/master/boot-neo4j). We faced the same situation as MongoDB, Spring Initializr does not generate a `TestcontainersConfiguration` class for Neo4j when adding `Neo4j`, so you need to create it yourself, or copy it from the generated result when add `Spring Data Neo4j` dependency into projects.

