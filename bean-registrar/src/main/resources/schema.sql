CREATE TABLE IF NOT EXISTS posts
(
    id      UUID,
    title   VARCHAR(255),
    content VARCHAR(1000)
);

ALTER TABLE posts
    ADD CONSTRAINT posts_pk PRIMARY KEY (id);