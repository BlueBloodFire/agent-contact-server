-- 为 agent_model_config 添加 user_id，改为按用户隔离
ALTER TABLE agent_model_config
    ADD COLUMN user_id VARCHAR(64) NOT NULL DEFAULT 'default' AFTER id;

-- 替换唯一索引为 (user_id, agent_id)
ALTER TABLE agent_model_config DROP INDEX uk_agent_id;
ALTER TABLE agent_model_config ADD UNIQUE KEY uk_user_agent (user_id, agent_id);
