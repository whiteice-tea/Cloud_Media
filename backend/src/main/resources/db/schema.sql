-- Cloud Media Platform schema (MySQL 8)
-- Charset: utf8mb4

CREATE DATABASE IF NOT EXISTS cloud_media
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE cloud_media;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(128) DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uniq_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS media_files (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  media_type ENUM('VIDEO','DOC') NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  ext VARCHAR(16) NOT NULL,
  size_bytes BIGINT NOT NULL,
  mime_type VARCHAR(128) NOT NULL,
  storage_rel_path VARCHAR(512) NOT NULL,
  derived_pdf_rel_path VARCHAR(512) DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_media_user (user_id),
  KEY idx_media_user_type (user_id, media_type),
  CONSTRAINT fk_media_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS video_progress (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  video_file_id BIGINT NOT NULL,
  progress_seconds INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uniq_user_video (user_id, video_file_id),
  KEY idx_progress_user (user_id),
  KEY idx_progress_video (video_file_id),
  CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_progress_video FOREIGN KEY (video_file_id) REFERENCES media_files (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
