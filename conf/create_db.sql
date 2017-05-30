DROP SCHEMA IF EXISTS Eva;
CREATE SCHEMA IF NOT EXISTS Eva;
USE Eva;

CREATE TABLE users (
	id BIGINT AUTO_INCREMENT,
	username VARCHAR(30) UNIQUE NOT NULL,
	mail VARCHAR(100) NOT NULL,
	password TEXT NOT NULL,
	rank INT DEFAULT 0,
    CONSTRAINT user_pk PRIMARY KEY (id)
);

CREATE TABLE post (
	id BIGINT AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    image_path TEXT NOT NULL,
    score BIGINT NOT NULL,
    nsfw BOOLEAN NOT NULL,
    owner_id BIGINT NOT NULL,
    CONSTRAINT user_pk PRIMARY KEY (id),
    CONSTRAINT user_post_fk FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE tag (
	id BIGINT AUTO_INCREMENT,
    name VARCHAR(30),
    CONSTRAINT tag_pk PRIMARY KEY (id)
);

CREATE TABLE tag_post (
	post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    CONSTRAINT tag_post_post_fk FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CONSTRAINT tag_post_tag_fk FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
);

CREATE TABLE user_session(
	id BIGINT AUTO_INCREMENT,
    session VARCHAR(200) UNIQUE NOT NULL,
    expires DATE NOT NULL,
    user_id BIGINT NOT NULL,
	CONSTRAINT user_session_pk PRIMARY KEY (id),
    CONSTRAINT user_session_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE comment(
	id BIGINT AUTO_INCREMENT,
    username VARCHAR(30) NOT NULL,
    post_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    CONSTRAINT comment_pk PRIMARY KEY (id),
	CONSTRAINT comment_post_fk FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
);

CREATE TABLE message(
	id BIGINT AUTO_INCREMENT,
    content TEXT NOT NULL,
    viewed BOOLEAN NOT NULL,
    user_blocked BOOLEAN NOT NULL,
    date DATE NOT NULL, 
	first_id BIGINT NOT NULL,
    second_id BIGINT NOT NULL,
    CONSTRAINT message_pk PRIMARY KEY (id),
	CONSTRAINT message1_user_fk FOREIGN KEY (first_id) REFERENCES users(id) ON DELETE CASCADE,
	CONSTRAINT message2_user_fk FOREIGN KEY (second_id) REFERENCES users(id) ON DELETE CASCADE
)