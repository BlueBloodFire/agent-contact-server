package cn.wjagent.ai.domain.agent.service;

import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;

import java.util.List;

public interface IArmoryService {

    void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception;

}
