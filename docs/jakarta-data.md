# Integrating Jakarta Data with Spring: Rinse and Repeat

In a previous post — [Integrating Jakarta Data with Spring](https://itnext.io/integrating-jakarta-data-with-spring-0beb5c215f5f) — we discussed how to integrate Jakarta Data with the Spring Framework. In that post we extracted Hibernate's `StatelessSession` from Spring's `LocalContainerEntityManagerFactoryBean` and registered it as a Spring bean. That approach worked, but it didn't allow us to use Spring's transaction manager to control transactions. We also noted a few ad-hoc workarounds, for example this [gist](https://gist.github.com/jelies/5181262) and [Vlad Mihalcea's writeup — How to integrate Jakarta Data with Spring and Hibernate](https://vladmihalcea.com/jakarta-data-spring-hibernate/).

In Spring 7, the long-awaited support for Hibernate's `StatelessSession` (see issue https://github.com/spring-projects/spring-framework/issues/7184) has been resovled finally. This means we can now use Spring's transaction manager to manage Jakarta Data transactions seamlessly.

The code snippets below target Spring 7.0 and Hibernate 7.2. See the complete example project at [spring7-sandbox/hibernate-jakarta-data](https://github.com/hantsy/spring7-sandbox/tree/master/hibernate-jakarta-data).

Add the following Hibernate configuration to your Spring project:

```java
@Configuration
@EnableTransactionManagement
@PropertySource(value = "classpath:/hibernate.properties", ignoreResourceNotFound = true)
public class JakartaDataConfig {

    public static final Logger log = LoggerFactory.getLogger(JakartaDataConfig.class);

    private static final String ENV_HIBERNATE_DIALECT = "hibernate.dialect";
    private static final String ENV_HIBERNATE_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";
    private static final String ENV_HIBERNATE_SHOW_SQL = "hibernate.show_sql";
    private static final String ENV_HIBERNATE_FORMAT_SQL = "hibernate.format_sql";

    @Autowired
    Environment env;

    @Bean
    LocalSessionFactoryBean sessionFactoryBean(DataSource dataSource) {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setPackagesToScan("com.example.demo");
        sessionFactory.setHibernateProperties(hibernateProperties());
        return sessionFactory;
    }

    private Properties hibernateProperties() {
        Properties extraProperties = new Properties();
        extraProperties.put(ENV_HIBERNATE_FORMAT_SQL, env.getProperty(ENV_HIBERNATE_FORMAT_SQL));
        extraProperties.put(ENV_HIBERNATE_SHOW_SQL, env.getProperty(ENV_HIBERNATE_SHOW_SQL));
        extraProperties.put(ENV_HIBERNATE_HBM2DDL_AUTO, env.getProperty(ENV_HIBERNATE_HBM2DDL_AUTO));

        if (env.getProperty(ENV_HIBERNATE_DIALECT) != null) {
            log.debug("Hibernate Dialect: {}", env.getProperty(ENV_HIBERNATE_DIALECT));
            extraProperties.put(ENV_HIBERNATE_DIALECT, env.getProperty(ENV_HIBERNATE_DIALECT));
        }

        return extraProperties;
    }

    @Bean
    public HibernateTransactionManager hibernateTransactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }

}
```

In the configuration above we declare a `LocalSessionFactoryBean` and a `HibernateTransactionManager`, which is the same pattern used for standard Hibernate integration with Spring. Behind the scenes, `LocalSessionFactoryBean` provides both `Session` and `StatelessSession`. The `@EnableTransactionManagement` annotation together with `HibernateTransactionManager` enables Spring's transaction management support.

Now create a Jakarta Data repository interface as follows:

```java
@jakarta.data.repository.Repository
@Transactional
public interface JakartaDataPostRepository {

    @Delete
    int deleteAll();

    @Save
    Post save(Post post);

    @Transactional(readOnly = true)
    @Find
    List<Post> findAll();

    @Transactional(readOnly = true)
    @Query("from Post p where p.title like :s and p.status=:status")
    List<Post> findByKeyword(@Param("s") String s, @Param("status") Status status, Limit limit);

    @Transactional(readOnly = true)
    @Find
    Optional<Post> findById(UUID id);
}
```

We add Jakarta Data's `@Repository` to mark the interface as a repository and use Spring's `@Transactional` to enable transaction management. We do not extend any Jakarta Data base interface; instead we use Jakarta Data's lifecycle-aware annotations on the methods.

> [!NOTE]
> Adding `@Transactional` at the repository interface level binds an active session to the current transaction. Alternatively, in a web application you can enable "Open Session In View" globally to keep the session open for the duration of the request.

When the interface is compiled, the generated implementation looks like this:

```java
@Component
public class JakartaDataPostRepository_ implements JakartaDataPostRepository {
	/**
	 * @see #findByKeyword(String,Status,Limit)
	 **/
	static final String FIND_BY_KEYWORD_String_Status = "from Post p where p.title like :s and p.status=:status";

	protected ObjectProvider<StatelessSession> session;
	
	public JakartaDataPostRepository_(ObjectProvider<StatelessSession> session) {
		this.session = session;
	}
	
	public StatelessSession session() {
		return session.getObject();
	}
	
	@Override
	public Post save(Post post) {
		requireNonNull(post, "Null post");
		try {
			if (session.getObject().getIdentifier(post) == null)
				session.getObject().insert(post);
			else
				session.getObject().upsert(post);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return post;
	}
//... other methods
}
```

The generated implementation `JakartaDataPostRepository_` is annotated with Spring's `@Component`, so it will be discovered and registered as a bean. The `StatelessSession` is injected via Spring's `ObjectProvider`, which supplies the current active session.

> [!NOTE]
> Don't forget to add `hibernate-processor` to the compiler's annotation-processor path to enable code generation.

The following test uses Spring Test and Testcontainers to verify the integration:

```java
@SpringJUnitConfig(classes = {
        JakartaDataPostRepositoryWithTestcontainersTest.TestConfig.class
})
@ContextConfiguration(initializers = PostgresContainerInitializer.class)
public class JakartaDataPostRepositoryWithTestcontainersTest {
    private final static Logger log = LoggerFactory.getLogger(JakartaDataPostRepositoryWithTestcontainersTest.class);

    @Autowired
    JakartaDataPostRepository posts;

    @BeforeEach
    public void setup() {
        var deleted = this.posts.deleteAll();
        log.debug("deleted posts: {}", deleted);
    }

    @Test
    public void testSaveAll() {
        var data = List.of(
                Post.of("test", "content", Status.PENDING_MODERATION),
                Post.of("test1", "content1", Status.DRAFT)
        );
        data.forEach(this.posts::save);

        var results = posts.findAll();
        assertThat(results.size()).isEqualTo(2);

        var resultsByKeyword = posts.findByKeyword("%", Status.PENDING_MODERATION, new Limit(10, 1));
        assertThat(resultsByKeyword.size()).isEqualTo(1);
    }

    @Test
    public void testInsertAndQuery() {
        var data = Post.of("test1", "content1", Status.DRAFT);
        var saved = this.posts.save(data);

        var byId = this.posts.findById(saved.getId());
        assertThat(byId.isPresent()).isTrue();

        var p = byId.get();
        assertThat(p.getStatus()).isEqualTo(Status.DRAFT);
    }

    @Configuration
    @ComponentScan(basePackageClasses = JakartaDataPostRepository.class)
    @Import({DataSourceConfig.class, JakartaDataConfig.class})
    static class TestConfig {
    }

}
```

In the test class we use `@SpringJUnitConfig` to load the Spring context and `@ContextConfiguration` to initialize the PostgreSQL Testcontainer. The tests verify the repository's basic CRUD operations.

For brevity we omitted the `Post` entity and the `DataSourceConfig` class, and we didn't list project dependencies. For the full example, see [spring7-sandbox/hibernate-jakarta-data](https://github.com/hantsylabs/spring7-sandbox/tree/main/hibernate-jakarta-data).