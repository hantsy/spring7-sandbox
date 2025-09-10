package com.example.demo;

import com.example.demo.repository.jpa.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan
@Configuration
public class Application {
    public static final Logger log  = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        var context = new AnnotationConfigApplicationContext(Application.class);
        var posts = context.getBean(PostRepository.class);
        posts.findAll().forEach(p -> log.debug("saved post:{}", p));
    }
}
