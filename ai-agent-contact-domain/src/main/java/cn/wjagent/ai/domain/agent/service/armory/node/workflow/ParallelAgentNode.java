package cn.wjagent.ai.domain.agent.service.armory.node.workflow;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.wjagent.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.wjagent.ai.domain.agent.model.valobj.enums.AgentTypeEnum;
import cn.wjagent.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.wjagent.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.ParallelAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Service("parallelAgentNode")
@Slf4j
public class ParallelAgentNode extends AbstractArmorySupport {

    @Override
    protected void multiThread(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        super.multiThread(requestParameter, dynamicContext);
    }

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParamter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {

        log.info("Ai Agent 装配操作 - ParallelAgentNode");

        AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow = dynamicContext.getCurrentAgentWorkflow();

        List<BaseAgent> subAgents = dynamicContext.queryAgentList(currentAgentWorkflow.getSubAgents());

        ParallelAgent parallelAgent = ParallelAgent.builder()
                .name(currentAgentWorkflow.getName())
                .description(currentAgentWorkflow.getDescription())
                .subAgents(subAgents)
                .build();

        dynamicContext.getAgentGroup().put(currentAgentWorkflow.getName(), parallelAgent);

        return router(requestParamter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {

        return getBean("agentWorkflowNode");

    }
}
