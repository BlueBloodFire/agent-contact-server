package cn.wjagent.ai.domain.agent.service.armory.factory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.wjagent.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.wjagent.ai.domain.agent.service.armory.node.RootNode;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DefaultArmoryFactory {

    @Resource
    private RootNode rootNode;

    @Autowired
    private ApplicationContext applicationContext;

    public StrategyHandler<ArmoryCommandEntity, DynamicContext, AiAgentRegisterVO> armoryStrategyHandler() {
        return rootNode;
    }

    public AiAgentRegisterVO getAiAgentRegisterVO(String agentId) {
        try {
            return applicationContext.getBean(agentId, AiAgentRegisterVO.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        private ChatModel chatModel;
        // 添加openAi节点设置
        private OpenAiApi openAiApi;

        /**
         * 原子安全的递进步骤
         */
        private AtomicInteger currentStepIndex = new AtomicInteger(0);

        /**
         * 当前的智能体
         */
        private AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow;

        private Map<String, BaseAgent> agentGroup = new HashMap<>();

        private Map<String, Object> dataObjects = new HashMap<>();

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }

        public List<BaseAgent> queryAgentList(List<String> agentNames) {
            if (agentNames == null || agentNames.isEmpty() || agentGroup == null) {
                return Collections.emptyList();
            }

            List<BaseAgent> agents = new ArrayList<>();
            for (String name : agentNames) {
                BaseAgent agent = agentGroup.get(name);
                if (agent != null) {
                    agents.add(agent);
                }
            }
            return agents;
        }

        public void addCurrentStepIndex() {
            currentStepIndex.incrementAndGet();
        }

        public int getCuurentStepIndex() {
            return currentStepIndex.get();
        }
    }
}
