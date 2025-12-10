-- 为 user_info 表添加 avatar 和 nick_name 字段

USE lib_manage;

-- 添加头像字段
ALTER TABLE `user_info` 
ADD COLUMN `avatar` VARCHAR(255) DEFAULT 'default.jpg' COMMENT '用户头像文件名' AFTER `password_hash`;

-- 添加昵称字段
ALTER TABLE `user_info` 
ADD COLUMN `nick_name` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称' AFTER `user_name`;

-- 更新现有用户的默认头像
UPDATE `user_info` SET `avatar` = 'default.jpg' WHERE `avatar` IS NULL;