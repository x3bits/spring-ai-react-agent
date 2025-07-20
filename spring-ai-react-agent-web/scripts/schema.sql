CREATE TABLE `threads` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
  `user_id` varchar(255) NOT NULL COMMENT '用户ID，标识线程所属用户',
  `thread_id` varchar(255) NOT NULL COMMENT '线程ID，全局唯一标识符',
  `title` varchar(500) NOT NULL COMMENT '线程标题，最大500字符',
  `agent` varchar(255) NOT NULL COMMENT '代理标识，用于区分不同的AI代理',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自动设置',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动设置',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `thread_id` (`thread_id`),
  KEY `idx_threads_user_id_agent` (`user_id`,`agent`) COMMENT '用户ID和代理联合索引，优化按用户和代理查询线程的性能',
  KEY `idx_threads_thread_id` (`thread_id`) COMMENT '线程ID索引，优化按线程ID查询的性能'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对话线程表，存储用户的聊天线程信息'