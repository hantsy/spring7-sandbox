package com.example.demo;

import java.time.Instant;

public record Greeting(String message, Instant sentAt) {
    public static Object of(String message) {
        return new Greeting(message, Instant.now());
    }
}
