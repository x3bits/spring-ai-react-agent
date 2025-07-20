-- H2 Database Schema for Testing
-- 删除表（如果存在）
DROP TABLE IF EXISTS threads;

-- 创建threads表
CREATE TABLE threads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    thread_id VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    agent VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引优化查询性能
CREATE INDEX idx_threads_user_id_agent ON threads(user_id, agent);

CREATE INDEX idx_threads_thread_id ON threads(thread_id);