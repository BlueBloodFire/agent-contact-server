package cn.wjagent.ai.test.langchain4j;

import cn.wjagent.ai.test.TestApiConfig;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * LangChain4j — Agent 测试
 * AiServices + 系统指令 + 工具 + 会话记忆，构成可多轮对话、自主调用工具的 Agent
 */
@Slf4j
public class LangChain4jAgentTest {

    /** Agent 可用的运维工具（有状态 mock：重启后状态变为 running） */
    static class OpsTools {

        private final java.util.Map<String, String> serviceStatus = new java.util.HashMap<>(
                java.util.Map.of("nginx", "running", "mysql", "stopped"));

        @Tool("查询指定服务的运行状态，返回 running/stopped")
        public String getServiceStatus(@P("服务名，如 nginx、mysql") String serviceName) {
            String status = serviceStatus.getOrDefault(serviceName.toLowerCase(), "stopped");
            log.info("[Tool 被调用] getServiceStatus({}) -> {}", serviceName, status);
            return status;
        }

        @Tool("重启指定服务，返回操作结果")
        public String restartService(@P("服务名") String serviceName) {
            log.info("[Tool 被调用] restartService({})", serviceName);
            serviceStatus.put(serviceName.toLowerCase(), "running");
            return "服务 " + serviceName + " 重启成功";
        }
    }

    /** Agent 接口：系统指令通过 @SystemMessage 注入 */
    interface OpsAgent {
        @SystemMessage("你是一个 Linux 运维 Agent。收到问题后先用工具查询状态，发现服务停止时主动重启并汇报结果。回答用中文，简洁。")
        String chat(String message);
    }

    public static void main(String[] args) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(TestApiConfig.BASE_URL + "/v1")
                .apiKey(TestApiConfig.API_KEY)
                .modelName(TestApiConfig.MODEL)
                .build();

        OpsAgent agent = AiServices.builder(OpsAgent.class)
                .chatModel(chatModel)
                .tools(new OpsTools())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 第一轮：Agent 应该先查状态，发现 mysql 停止后调用重启工具
        String round1 = agent.chat("帮我看下 mysql 服务怎么样，有问题就处理掉");
        log.info("第一轮结果:{}", round1);

        // 第二轮：验证记忆，Agent 应记得上一轮处理过 mysql
        String round2 = agent.chat("刚才你处理的是哪个服务？");
        log.info("第二轮结果:{}", round2);
    }
}
