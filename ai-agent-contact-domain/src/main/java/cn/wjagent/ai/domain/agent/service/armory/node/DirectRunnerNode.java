package cn.wjagent.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.wjagent.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.wjagent.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.wjagent.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Direct 模式节点：绕过 ADK，直接用 Spring AI ChatModel 做流式输出。
 * 叶子节点，不再向下路由。
 */
@Slf4j
@Service
public class DirectRunnerNode extends AbstractArmorySupport {

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter,
                                        DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - DirectRunnerNode (direct 模式)");

        AiAgentConfigTableVO config = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Agent agent = config.getAgent();
        AiAgentConfigTableVO.Module module = config.getModule();

        AiAgentRegisterVO vo = AiAgentRegisterVO.builder()
                .appName(config.getAppName())
                .agentId(agent.getAgentId())
                .agentName(agent.getAgentName())
                .agentDesc(agent.getAgentDesc())
                .chatModel(dynamicContext.getChatModel())
                .systemPrompt(module.getSystemPrompt())
                .directMode(true)
                .build();

        registerBean(agent.getAgentId(), AiAgentRegisterVO.class, vo);
        return vo;
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(
            ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }

    @Override
    protected void multiThread(ArmoryCommandEntity requestParameter,
                               DefaultArmoryFactory.DynamicContext dynamicContext)
            throws ExecutionException, InterruptedException, TimeoutException {
        super.multiThread(requestParameter, dynamicContext);
    }
}
