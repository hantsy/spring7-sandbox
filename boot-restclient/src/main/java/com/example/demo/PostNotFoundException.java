package com.example.demo;

import java.util.UUID;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(UUID uuid) {
        super("Could not find post with id: " + uuid);
    }
}
