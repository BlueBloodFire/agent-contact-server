package cn.wjagent.ai.domain.agent.service.armory.factory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.wjagent.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.wjagent.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import cn.wjagent.ai.domain.agent.service.armory.node.RootNode;
import com.google.adk.agents.BaseAgent;
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

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DefaultArmoryFactory {

    /** 用户级模型配置缓存，key = userId:agentId */
    private final Map<String, ChatModel> userChatModels = new ConcurrentHashMap<>();

    @Resource
    private RootNode rootNode;

    @Autowired
    private ApplicationContext applicationContext;

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

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

    /** 为指定用户构建并缓存个人模型，不影响全局配置 */
    public void updateUserModelConfig(String userId, String agentId, String baseUrl, String apiKey, String model) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
        userChatModels.put(userId + ":" + agentId, chatModel);
    }

    /** 获取用户个人模型，不存在则返回 null（调用方回退到全局配置） */
    public ChatModel getUserChatModel(String userId, String agentId) {
        return userChatModels.get(userId + ":" + agentId);
    }

    public void updateModelConfig(String agentId, String baseUrl, String apiKey, String model) throws Exception {
        Map<String, AiAgentConfigTableVO> tables = aiAgentAutoConfigProperties.getTables();
        AiAgentConfigTableVO targetTable = null;
        for (AiAgentConfigTableVO table : tables.values()) {
            if (table.getAgent() != null && agentId.equals(table.getAgent().getAgentId())) {
                targetTable = table;
                break;
            }
        }
        if (targetTable == null) {
            throw new IllegalArgumentException("未找到 agentId: " + agentId);
        }
        AiAgentConfigTableVO.Module.AiApi aiApi = targetTable.getModule().getAiApi();
        if (baseUrl != null && !baseUrl.isBlank()) aiApi.setBaseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) aiApi.setApiKey(apiKey);
        if (model != null && !model.isBlank()) targetTable.getModule().getChatModel().setModel(model);
        // 直接调用策略链重新装配，避免注入 IArmoryService 导致循环依赖
        armoryStrategyHandler().apply(
                ArmoryCommandEntity.builder().aiAgentConfigTableVO(targetTable).build(),
                new DynamicContext());
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
