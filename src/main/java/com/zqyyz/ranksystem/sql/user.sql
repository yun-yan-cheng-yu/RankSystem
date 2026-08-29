CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4;

USE user_db;

CREATE TABLE IF NOT EXISTS `user` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_name` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `age` INT COMMENT '年龄',
    `email` VARCHAR(100) COMMENT '邮箱',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试数据
INSERT INTO `user` (user_name, password, age, email) VALUES
('张三', '123456', 25, 'zhangsan@test.com'),
('李四', '654321', 30, 'lisi@test.com'),
('王五', '111111', 28, 'wangwu@test.com');