CREATE TABLE IF NOT EXISTS posts
(
    id      UUID          NOT NULL DEFAULT random_uuid(),
    title   VARCHAR(255)  NOT NULL,
    content VARCHAR(1000) NOT NULL
);

ALTER TABLE posts
    ADD CONSTRAINT posts_pk PRIMARY KEY (id);