package cn.wjagent.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.wjagent.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.wjagent.ai.domain.agent.model.valobj.enums.AgentTypeEnum;
import cn.wjagent.ai.domain.agent.service.armory.AbstractArmorySupport;
import cn.wjagent.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.wjagent.ai.domain.agent.service.armory.node.workflow.LoopAgentNode;
import cn.wjagent.ai.domain.agent.service.armory.node.workflow.ParallelAgentNode;
import cn.wjagent.ai.domain.agent.service.armory.node.workflow.SequentialAgentNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class AgentWorkflowNode extends AbstractArmorySupport {

    @Resource
    private LoopAgentNode loopAgentNode;

    @Resource
    private ParallelAgentNode parallelAgentNode;

    @Resource
    private SequentialAgentNode sequentialAgentNode;

    @Resource
    private RunnerNode runnerNode;

    @Override
    protected void multiThread(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        super.multiThread(requestParameter, dynamicContext);
    }

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParamter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AgentWorkflowNode");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParamter.getAiAgentConfigTableVO();
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = aiAgentConfigTableVO.getModule().getAgentWorkflows();

        if (null == agentWorkflows || agentWorkflows.isEmpty() || dynamicContext.getCurrentStepIndex().get() >= agentWorkflows.size()) {
            // 设置结果值
            dynamicContext.setCurrentAgentWorkflow(null);
            // 路由下节点
            return router(requestParamter, dynamicContext);
        }

        // 设置当前判断流程对象
        dynamicContext.setCurrentAgentWorkflow(agentWorkflows.get(dynamicContext.getCurrentStepIndex().get()));

        // 步骤值增加
        dynamicContext.addCurrentStepIndex();

        return router(requestParamter, dynamicContext);

    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {

        AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow = dynamicContext.getCurrentAgentWorkflow();

        if (null == currentAgentWorkflow) {
            return runnerNode;
        }

        String type = currentAgentWorkflow.getType();
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.fromType(type);

        if(null == agentTypeEnum) {
            throw new RuntimeException("agentWorkflows type is error");
        }

        String node = agentTypeEnum.getNode();

        return switch (node) {
            case "loopAgentNode" -> loopAgentNode;
            case "parallelAgentNode" -> parallelAgentNode;
            case "sequentialAgentNode" -> sequentialAgentNode;
            default -> runnerNode;
        };
    }
}
