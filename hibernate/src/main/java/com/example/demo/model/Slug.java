package com.example.demo.model;

import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Objects;

@Embeddable
public record Slug(String slug) {

    public static Slug deriveFromTitle(String title) {
        Objects.requireNonNull(title, "title is null");
        var trimmedTitle = title.trim() // remove head/tail spaces
                .toLowerCase() // to lower case
                .replaceAll("\\s", "-") // repace all spaces with -
                + "-"
                + Instant.now().getEpochSecond();
        return new Slug(trimmedTitle);
    }
}
