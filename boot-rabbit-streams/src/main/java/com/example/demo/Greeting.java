package com.example.demo;

import java.time.Instant;

record Greeting(String message, Instant sent) {
    public static Greeting of(String message) {
        return new Greeting(message, Instant.now());
    }
}
