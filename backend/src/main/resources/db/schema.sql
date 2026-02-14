-- Cloud Media schema (MySQL 8)
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
  id VARCHAR(36) NOT NULL,
  guest_id VARCHAR(64) NOT NULL,
  media_type ENUM('VIDEO','DOC') NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  ext VARCHAR(16) NOT NULL,
  path VARCHAR(1024) NOT NULL,
  derived_pdf_rel_path VARCHAR(1024) DEFAULT NULL,
  mime_type VARCHAR(128) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  expires_at DATETIME NOT NULL,
  status ENUM('ACTIVE','DELETED') NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (id),
  KEY idx_guest_active (guest_id, status, expires_at),
  KEY idx_expires (expires_at, status),
  KEY idx_guest_type (guest_id, media_type, status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS video_progress (
  id BIGINT NOT NULL AUTO_INCREMENT,
  guest_id VARCHAR(64) NOT NULL,
  video_file_id VARCHAR(36) NOT NULL,
  progress_seconds INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uniq_guest_video (guest_id, video_file_id),
  KEY idx_video_progress_guest (guest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
