package cn.wjagent.ai.infrastructure.dao;

import cn.wjagent.ai.infrastructure.po.ChatMessagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageDao {

    void insert(ChatMessagePO po);

    List<ChatMessagePO> findBySessionId(@Param("sessionId") String sessionId);

    List<ChatMessagePO> findBySessionIdWithLimit(@Param("sessionId") String sessionId, @Param("limit") int limit);

    int countBySessionId(@Param("sessionId") String sessionId);
}
