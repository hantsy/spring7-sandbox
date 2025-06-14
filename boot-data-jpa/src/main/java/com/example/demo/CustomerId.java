package com.example.demo;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record CustomerId(UUID id) {
    public CustomerId() {
        this(UUID.randomUUID());
    }
}
