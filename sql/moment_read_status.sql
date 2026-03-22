-- 创建moment_read_status表来跟踪用户的最后查看时间
CREATE TABLE IF NOT EXISTS moment_read_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    last_read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 为user_id创建索引以提高查询性能
CREATE INDEX idx_moment_read_status_user_id ON moment_read_status(user_id);
