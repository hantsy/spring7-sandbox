package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ApiVersionInserter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = GreetingController.class)
public class GreetingControllerTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        MockMvcBuilderCustomizer mockMvcBuilderCustomizer() {
            return builder -> builder
                    .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                    .build();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void testHello() throws Exception {
        this.mockMvc.perform(get("/hello")/*.apiVersion("1.0")*/)
                .andExpect(content().string("Hello v1.0(Default)"));
    }

    @Test
    void testHello1_1() throws Exception {
        this.mockMvc.perform(get("/hello").apiVersion("1.1"))
                .andExpect(content().string("Hello v1.1"));
    }

    @Test
    void testHello2_0() throws Exception {
        this.mockMvc.perform(get("/hello").apiVersion("2.0"))
                .andExpect(content().string("Hello v2.0"));
    }
}
