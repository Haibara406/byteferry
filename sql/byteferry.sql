/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : byteferry

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 18/03/2026 17:23:55
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for friend_session_history
-- ----------------------------
DROP TABLE IF EXISTS `friend_session_history`;
CREATE TABLE `friend_session_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `closed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `expire_seconds` int DEFAULT NULL,
  `item_count` int DEFAULT NULL,
  `session_id` varchar(36) NOT NULL,
  `admin_username` varchar(50) NOT NULL,
  `participant_count` int DEFAULT NULL,
  `participants` varchar(500) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `items_json` mediumtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for friendships
-- ----------------------------
DROP TABLE IF EXISTS `friendships`;
CREATE TABLE `friendships` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `accepted_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `friend_id` bigint NOT NULL,
  `status` enum('ACCEPTED','BLOCKED','PENDING') NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjwaac0iw9d1fu58mx7afwf9f4` (`user_id`,`friend_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for space_files
-- ----------------------------
DROP TABLE IF EXISTS `space_files`;
CREATE TABLE `space_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) DEFAULT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `mime_type` varchar(100) DEFAULT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgr65d4jmy4a8q2csinporj30d` (`item_id`),
  CONSTRAINT `FKgr65d4jmy4a8q2csinporj30d` FOREIGN KEY (`item_id`) REFERENCES `space_items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for space_items
-- ----------------------------
DROP TABLE IF EXISTS `space_items`;
CREATE TABLE `space_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `created_at` datetime(6) NOT NULL,
  `type` enum('FILE','IMAGE','TEXT') NOT NULL,
  `user_id` bigint NOT NULL,
  `expire_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `password` varchar(255) NOT NULL,
  `username` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
