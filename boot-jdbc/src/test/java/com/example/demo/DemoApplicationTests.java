package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
