package com.example.demo.model;

import jakarta.persistence.EnumeratedValue;

public enum Status {
    DRAFT(-1), PENDING_MODERATION(0), PUBLISHED(1);

    @EnumeratedValue
    private final int code;

    Status(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
