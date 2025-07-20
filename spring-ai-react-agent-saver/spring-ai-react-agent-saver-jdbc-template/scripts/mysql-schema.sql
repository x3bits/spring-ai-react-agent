CREATE TABLE `message_branch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键，用于排序',
  `message_id` varchar(64) NOT NULL COMMENT '消息唯一标识符',
  `thread_id` varchar(64) NOT NULL COMMENT '线程ID',
  `previous_id` varchar(64) DEFAULT NULL COMMENT '前一个消息ID，形成链式结构',
  `message_type` varchar(20) NOT NULL COMMENT '消息类型：USER/ASSISTANT/SYSTEM/TOOL',
  `message_content` text NOT NULL COMMENT '消息内容JSON',
  `depth` int NOT NULL DEFAULT '0' COMMENT '消息在树中的深度',
  `ancestor_path` text COMMENT '祖先路径，用逗号分隔的ID列表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（用于运维工具）',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（用于运维工具）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `message_id` (`message_id`),
  KEY `idx_thread_id` (`thread_id`) COMMENT '线程查询索引',
  KEY `idx_previous_id` (`previous_id`) COMMENT '前置消息查询索引',
  KEY `idx_thread_depth` (`thread_id`,`depth`) COMMENT '线程深度查询索引',
  KEY `idx_depth` (`depth`) COMMENT '深度查询索引',
  KEY `idx_thread_id_order` (`thread_id`,`id`) COMMENT '线程消息排序索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息分支存储表'