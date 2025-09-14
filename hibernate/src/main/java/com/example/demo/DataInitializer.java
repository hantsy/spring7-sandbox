package com.example.demo;

import com.example.demo.model.Post;
import com.example.demo.model.Status;
import com.example.demo.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class DataInitializer {
    public static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PostRepository posts;

    public DataInitializer(PostRepository posts) {
        this.posts = posts;
    }

    @EventListener(value = ContextRefreshedEvent.class)
    public void init() throws Exception {
        log.info("start data initialization...");
        Stream.of("one", "two")
                .map(s -> Post.of("Post " + s, "Content of post " + s, Status.DRAFT))
                .forEach(this.posts::save);
        this.posts.findAll().forEach(p -> log.debug("saved post:{}", p));
    }
}