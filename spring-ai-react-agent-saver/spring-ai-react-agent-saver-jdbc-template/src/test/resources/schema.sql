-- 消息分支存储表
CREATE TABLE IF NOT EXISTS message_branch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL UNIQUE,
    thread_id VARCHAR(64) NOT NULL,
    previous_id VARCHAR(64) NULL,
    message_type VARCHAR(20) NOT NULL,
    message_content TEXT NOT NULL,
    depth INT NOT NULL DEFAULT 0,
    ancestor_path TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_thread_id ON message_branch (thread_id);
CREATE INDEX IF NOT EXISTS idx_previous_id ON message_branch (previous_id);
CREATE INDEX IF NOT EXISTS idx_thread_depth ON message_branch (thread_id, depth);
CREATE INDEX IF NOT EXISTS idx_depth ON message_branch (depth);
CREATE INDEX IF NOT EXISTS idx_thread_id_order ON message_branch (thread_id, id); 