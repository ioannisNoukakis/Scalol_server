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
    session TEXT NOT NULL,
    expires DATE NOT NULL,
    user_id BIGINT NOT NULL,
	CONSTRAINT user_session_pk PRIMARY KEY (id),
    CONSTRAINT user_session_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

SELECT * FROM users INNER JOIN user_session AS us ON users.id = us.user_id;
SELECT * FROM user_session;
SELECT * FROM post;

INSERT INTO users (username, mail, password, rank) VALUES ("user1", "miaw", "1234",  0);