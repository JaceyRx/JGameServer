/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 50716
 Source Host           : localhost:3306
 Source Schema         : jgame_server

 Target Server Type    : MySQL
 Target Server Version : 50716
 File Encoding         : 65001

 Date: 29/06/2020 17:40:03
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for battle_record
-- ----------------------------
DROP TABLE IF EXISTS `battle_record`;
CREATE TABLE `battle_record`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `battle_type` int(11) NOT NULL,
  `battle_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `user_id_list` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '对战玩家userId列表（逗号分隔）',
  `battle_start_timestamp` timestamp(0) NULL DEFAULT NULL COMMENT '对战开始时间',
  `battle_end_timestamp` timestamp(0) NULL DEFAULT NULL COMMENT '对战结束事件',
  `turn_count` int(11) NULL DEFAULT NULL COMMENT '回合数',
  `winner_user_id` int(11) NULL DEFAULT NULL COMMENT '获胜玩家id',
  `game_over_reason` int(11) NULL DEFAULT NULL COMMENT '游戏结束原因',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for gm_user
-- ----------------------------
DROP TABLE IF EXISTS `gm_user`;
CREATE TABLE `gm_user`  (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `passwordMD5` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gm_user
-- ----------------------------
INSERT INTO `gm_user` VALUES (1, 'admin', '21232F297A57A5A743894A0E4A801FC3');

-- ----------------------------
-- Table structure for play_forbid
-- ----------------------------
DROP TABLE IF EXISTS `play_forbid`;
CREATE TABLE `play_forbid`  (
  `forbid_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT '所属用户id',
  `forbid_reason` int(11) NOT NULL COMMENT '封禁理由',
  `forbid_start_timestamp` datetime(0) NOT NULL COMMENT '封禁开始时间',
  `forbid_end_timestamp` datetime(0) NOT NULL COMMENT '封禁结束时间',
  PRIMARY KEY (`forbid_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for play_state
-- ----------------------------
DROP TABLE IF EXISTS `play_state`;
CREATE TABLE `play_state`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT '玩家id',
  `user_online_state` int(11) NOT NULL COMMENT '在线状态（在线or离线）',
  `user_action_state` int(11) NOT NULL COMMENT '行为状态（none or 匹配中 or 对战中 等）',
  `battle_type` int(11) NULL DEFAULT NULL COMMENT '对战类型（1v1 or ..）',
  `battle_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '对战id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for play_user
-- ----------------------------
DROP TABLE IF EXISTS `play_user`;
CREATE TABLE `play_user`  (
  `user_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户名名称',
  `nickname` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `passwordMD5` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'MD5加密密码',
  `regist_timestamp` timestamp(0) NULL DEFAULT NULL COMMENT '注册时间',
  `regist_ip` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '注册ip',
  `last_login_timestamp` timestamp(0) NULL DEFAULT NULL COMMENT '最后一次登录时间戳',
  `last_login_ip` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '最后一次登录ip',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
