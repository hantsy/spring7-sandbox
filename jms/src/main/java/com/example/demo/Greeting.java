package com.example.demo;

import java.time.Instant;

public record Greeting(String message, Instant sentAt) {
}
