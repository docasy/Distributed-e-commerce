-- ====================================
-- 分布式电商平台数据库初始化脚本
-- ====================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ecommerce_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ====================================
-- 用户服务数据库
-- ====================================
USE ecommerce_user;

DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（加密）',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(200) DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint DEFAULT '0' COMMENT '性别：0-未知 1-男 2-女',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用 1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入测试数据
INSERT INTO `tb_user` (`username`, `password`, `nickname`, `phone`, `email`, `gender`, `status`) VALUES
('testuser', 'e10adc3949ba59abbe56e057f20f883e', '测试用户', '13800138000', 'test@example.com', 1, 1),
('alice', 'e10adc3949ba59abbe56e057f20f883e', 'Alice', '13900139000', 'alice@example.com', 2, 1),
('bob', 'e10adc3949ba59abbe56e057f20f883e', 'Bob', '13700137000', 'bob@example.com', 1, 1);
-- 注意：密码是 '123456' 的MD5值

-- ====================================
-- 商品服务数据库
-- ====================================
USE ecommerce_product;

DROP TABLE IF EXISTS `tb_product`;
CREATE TABLE `tb_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '商品名称',
  `title` varchar(200) DEFAULT NULL COMMENT '商品标题',
  `description` text COMMENT '商品描述',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `brand` varchar(50) DEFAULT NULL COMMENT '品牌',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存数量',
  `sales` int DEFAULT '0' COMMENT '销量',
  `main_image` varchar(200) DEFAULT NULL COMMENT '主图URL',
  `images` text COMMENT '商品图片（JSON数组）',
  `status` tinyint DEFAULT '1' COMMENT '商品状态：0-下架 1-上架',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 插入测试数据
INSERT INTO `tb_product` (`name`, `title`, `description`, `brand`, `price`, `stock`, `sales`, `status`) VALUES
('iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB 黑色钛金属', '全新A17 Pro芯片，钛金属设计，专业级摄像系统', 'Apple', 8999.00, 100, 0, 1),
('MacBook Pro', 'MacBook Pro 14英寸 M3芯片 16GB 512GB', '强大M3芯片，Liquid Retina XDR显示屏', 'Apple', 14999.00, 50, 0, 1),
('AirPods Pro', 'AirPods Pro (第二代) USB-C', '主动降噪，空间音频，持久续航', 'Apple', 1899.00, 200, 0, 1),
('小米14 Ultra', '小米14 Ultra 16GB+512GB 黑色', '徕卡影像，骁龙8 Gen3，120W快充', '小米', 6499.00, 150, 0, 1),
('华为Mate 60 Pro', '华为Mate 60 Pro 12GB+512GB', '星闪通信，昆仑玻璃，北斗卫星消息', '华为', 6999.00, 80, 0, 1);

-- ====================================
-- 订单服务数据库
-- ====================================
USE ecommerce_order;

DROP TABLE IF EXISTS `tb_order`;
CREATE TABLE `tb_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(50) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) NOT NULL COMMENT '商品名称',
  `product_price` decimal(10,2) NOT NULL COMMENT '商品单价',
  `quantity` int NOT NULL COMMENT '购买数量',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `status` tinyint DEFAULT '0' COMMENT '订单状态：0-待支付 1-已支付 2-已取消 3-已完成 4-已关闭',
  `payment_method` tinyint DEFAULT NULL COMMENT '支付方式：1-支付宝 2-微信',
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `address` varchar(200) DEFAULT NULL COMMENT '收货地址',
  `receiver` varchar(50) DEFAULT NULL COMMENT '收货人',
  `receiver_phone` varchar(11) DEFAULT NULL COMMENT '收货人电话',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
