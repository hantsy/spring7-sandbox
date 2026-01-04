package com.example.demo;

import java.time.Instant;

public record Greeting(String body, Instant sentAt) {
}
