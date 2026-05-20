package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    public final static String DEMO_TOPIC_NAME = "demo-topic";
    public static final String DEMO_GROUP_NAME = "demo-group";

    public final static String DEMO_TOPIC_ANNOTATION_NAME = "demo-topic-anno";
    public static final String DEMO_GROUP_ANNOTATION_NAME = "demo-group-anno";

    public final static String DEMO_TOPIC_EXPLICIT_NAME = "demo-topic-explicit";
    public static final String DEMO_GROUP_EXPLICIT_NAME = "demo-group-explicit";
}

