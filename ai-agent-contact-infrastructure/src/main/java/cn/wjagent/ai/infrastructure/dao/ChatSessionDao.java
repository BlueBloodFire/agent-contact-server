package cn.wjagent.ai.infrastructure.dao;

import cn.wjagent.ai.infrastructure.po.ChatSessionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatSessionDao {

    void insert(ChatSessionPO po);

    ChatSessionPO findBySessionId(@Param("sessionId") String sessionId);

    List<ChatSessionPO> findByUserId(@Param("userId") String userId);

    List<ChatSessionPO> findByUserIdAndAgentId(@Param("userId") String userId, @Param("agentId") String agentId);

    void updateTurnCount(@Param("sessionId") String sessionId, @Param("turnCount") int turnCount);

    void updateTitle(@Param("sessionId") String sessionId, @Param("title") String title);
}
