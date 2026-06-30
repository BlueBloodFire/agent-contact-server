package cn.wjagent.ai.domain.agent.adapter.repository;

import java.util.List;

public interface IChatHistoryRepository {

    void saveSession(String sessionId, String agentId, String userId, String title);

    void saveMessage(String messageId, String sessionId, String agentId, String userId,
                     String role, String content);

    void updateSessionTurnCount(String sessionId, int turnCount);

    void updateSessionTitle(String sessionId, String title);

    /** Load message history for a session ordered by time ascending. */
    List<MessageRecord> loadMessages(String sessionId);

    class MessageRecord {
        public final String role;
        public final String content;

        public MessageRecord(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
