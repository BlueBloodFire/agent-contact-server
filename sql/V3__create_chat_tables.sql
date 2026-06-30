-- 会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id  VARCHAR(64)   NOT NULL UNIQUE COMMENT '会话唯一ID',
    agent_id    VARCHAR(32)   NOT NULL COMMENT '智能体ID',
    user_id     VARCHAR(64)   NOT NULL COMMENT '用户ID',
    title       VARCHAR(200)  DEFAULT '' COMMENT '会话标题',
    turn_count  INT           NOT NULL DEFAULT 0 COMMENT '对话轮次',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_agent (user_id, agent_id),
    INDEX idx_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话';

-- 消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id  VARCHAR(64)   NOT NULL UNIQUE COMMENT '消息唯一ID',
    session_id  VARCHAR(64)   NOT NULL COMMENT '所属会话ID',
    agent_id    VARCHAR(32)   NOT NULL COMMENT '智能体ID',
    user_id     VARCHAR(64)   NOT NULL COMMENT '用户ID',
    role        VARCHAR(16)   NOT NULL COMMENT 'user / assistant',
    content     LONGTEXT      NOT NULL COMMENT '消息内容',
    token_count INT           NOT NULL DEFAULT 0 COMMENT 'token数量',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息';
