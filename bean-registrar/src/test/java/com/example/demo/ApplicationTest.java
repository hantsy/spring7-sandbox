package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(CustomConfig.class)
class ApplicationTest {

    @Autowired
    PostRepository postRepository;

    @Test
    void testPostRepository() {
        assertThat(this.postRepository).isInstanceOf(InMemoryPostRepository.class);
    }
}
