package com.example.demo;

import com.example.demo.model.Post;
import com.example.demo.model.Status;
import com.example.demo.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hantsy
 */
@SpringJUnitConfig(classes = {PostRepositoryTest.TestConfig.class})
public class PostRepositoryTest {
    private final static Logger log = LoggerFactory.getLogger(PostRepositoryTest.class);

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
                Post.of("test1", "content1", Status.DRAFT)
        );
        data.forEach(this.posts::save);

        var results = posts.findAll();
        assertThat(results.size()).isEqualTo(2);

        var resultsByKeyword = posts.findByKeyword("", Status.PENDING_MODERATION, 0, 10);
        assertThat(resultsByKeyword.size()).isEqualTo(1);
    }

    @Test
    public void testInsertAndQuery() {
        var data = Post.of("test1", "content1", Status.DRAFT);
        var saved = this.posts.save(data);
        this.posts.findById(saved.getId()).ifPresent(
                p -> assertThat(p.getStatus()).isEqualTo(Status.DRAFT)
        );

    }

    @Configuration
    @ComponentScan(basePackageClasses = PostRepository.class)
    @Import({JpaConfig.class})
    static class TestConfig {

        @Bean
        @Primary
        public DataSource embeddedDataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }
    }

}
