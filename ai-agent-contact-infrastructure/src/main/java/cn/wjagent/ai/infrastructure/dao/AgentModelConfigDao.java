package cn.wjagent.ai.infrastructure.dao;

import cn.wjagent.ai.infrastructure.po.AgentModelConfigPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentModelConfigDao {

    void upsert(AgentModelConfigPO po);

    AgentModelConfigPO findByUserIdAndAgentId(@Param("userId") String userId, @Param("agentId") String agentId);

    List<AgentModelConfigPO> findAll();
}
