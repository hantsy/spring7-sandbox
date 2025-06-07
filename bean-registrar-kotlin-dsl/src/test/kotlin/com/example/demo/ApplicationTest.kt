package com.example.demo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(CustomConfig::class)
class ApplicationTest @Autowired constructor(private val postRepository: PostRepository) {

    @Test
    fun testPostRepository() {
        assertThat(this.postRepository).isInstanceOf(InMemoryPostRepository::class.java)
    }
}