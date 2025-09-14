package com.example.demo;

import com.example.demo.model.Post;
import com.example.demo.model.Status;
import com.example.demo.repository.data.JakartaDataPostRepository;
import com.example.demo.repository.jpa.PostRepository;
import jakarta.data.Limit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hantsy
 */
@SpringJUnitConfig(classes = {
        JakartaDataPostRepositoryTestWithTestcontainers.TestConfig.class
})
@ContextConfiguration(initializers = PostgresContainerInitializer.class)
public class JakartaDataPostRepositoryTestWithTestcontainers {
    private final static Logger log = LoggerFactory.getLogger(JakartaDataPostRepositoryTestWithTestcontainers.class);

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

        var resultsByKeyword = posts.findByKeyword("", Status.PENDING_MODERATION, new Limit(10, 0));
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
    @ComponentScan(basePackageClasses = JakartaDataPostRepository.class)
    @Import({DataSourceConfig.class, JakartaDataConfig.class})
    static class TestConfig {
    }

}
