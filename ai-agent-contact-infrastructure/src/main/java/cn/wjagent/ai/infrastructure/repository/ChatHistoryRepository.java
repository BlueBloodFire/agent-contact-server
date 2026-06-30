package cn.wjagent.ai.infrastructure.repository;

import cn.wjagent.ai.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.wjagent.ai.infrastructure.dao.ChatMessageDao;
import cn.wjagent.ai.infrastructure.dao.ChatSessionDao;
import cn.wjagent.ai.infrastructure.po.ChatMessagePO;
import cn.wjagent.ai.infrastructure.po.ChatSessionPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class ChatHistoryRepository implements IChatHistoryRepository {

    @Resource
    private ChatSessionDao chatSessionDao;

    @Resource
    private ChatMessageDao chatMessageDao;

    @Override
    public void saveSession(String sessionId, String agentId, String userId, String title) {
        try {
            chatSessionDao.insert(ChatSessionPO.builder()
                    .sessionId(sessionId)
                    .agentId(agentId)
                    .userId(userId)
                    .title(title)
                    .turnCount(0)
                    .build());
        } catch (Exception e) {
            log.warn("saveSession failed sessionId:{} {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void saveMessage(String messageId, String sessionId, String agentId, String userId,
                            String role, String content) {
        try {
            chatMessageDao.insert(ChatMessagePO.builder()
                    .messageId(messageId)
                    .sessionId(sessionId)
                    .agentId(agentId)
                    .userId(userId)
                    .role(role)
                    .content(content)
                    .tokenCount(content.length() / 4)
                    .build());
        } catch (Exception e) {
            log.warn("saveMessage failed sessionId:{} role:{} {}", sessionId, role, e.getMessage());
        }
    }

    @Override
    public void updateSessionTurnCount(String sessionId, int turnCount) {
        try {
            chatSessionDao.updateTurnCount(sessionId, turnCount);
        } catch (Exception e) {
            log.warn("updateSessionTurnCount failed sessionId:{} {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void updateSessionTitle(String sessionId, String title) {
        try {
            chatSessionDao.updateTitle(sessionId, title);
        } catch (Exception e) {
            log.warn("updateSessionTitle failed sessionId:{} {}", sessionId, e.getMessage());
        }
    }

    @Override
    public List<MessageRecord> loadMessages(String sessionId) {
        try {
            List<ChatMessagePO> pos = chatMessageDao.findBySessionId(sessionId);
            return pos.stream()
                    .map(p -> new MessageRecord(p.getRole(), p.getContent()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("loadMessages failed sessionId:{} {}", sessionId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
