package com.example.demo.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity()
@Table(name = "posts")
public class Post implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Embedded
    private Slug slug;

    // use new @EnumeratedValue
    private Status status = Status.DRAFT;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Version
    private Long version = 1L;

    public Post() {
    }

    public Post(UUID id, String title, String content, Status status, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.slug = Slug.deriveFromTitle(this.title);
    }

    public static Post of (String title, String content, Status status){
        return new Post(UUID.randomUUID(), title, content, status, LocalDateTime.now());
    }

    @PrePersist // prepersist callback
    public void beforePersist() {
        this.setCreatedAt(LocalDateTime.now());
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Slug getSlug() {
        return slug;
    }

    public void setSlug(Slug slug) {
        this.slug = slug;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(slug, post.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(slug);
    }
}