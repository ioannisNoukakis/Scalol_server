DROP SCHEMA IF EXISTS Eva;
CREATE SCHEMA IF NOT EXISTS Eva;
USE Eva;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(30) UNIQUE NOT NULL,
  mail VARCHAR(100) NOT NULL,
  rank INT
)