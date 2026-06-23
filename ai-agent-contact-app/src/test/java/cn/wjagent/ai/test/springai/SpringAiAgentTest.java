package cn.wjagent.ai.test.springai;

import cn.wjagent.ai.test.TestApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Spring AI — Agent 测试
 * ChatClient + 系统指令 + 工具 + 会话记忆，构成一个可多轮对话、自主调用工具的 Agent
 */
@Slf4j
public class SpringAiAgentTest {

    /** Agent 可用的运维工具（有状态 mock：重启后状态变为 running） */
    static class OpsTools {

        private final java.util.Map<String, String> serviceStatus = new java.util.HashMap<>(
                java.util.Map.of("nginx", "running", "mysql", "stopped"));

        @Tool(description = "查询指定服务的运行状态，返回 running/stopped")
        public String getServiceStatus(@ToolParam(description = "服务名，如 nginx、mysql") String serviceName) {
            String status = serviceStatus.getOrDefault(serviceName.toLowerCase(), "stopped");
            log.info("[Tool 被调用] getServiceStatus({}) -> {}", serviceName, status);
            return status;
        }

        @Tool(description = "重启指定服务，返回操作结果")
        public String restartService(@ToolParam(description = "服务名") String serviceName) {
            log.info("[Tool 被调用] restartService({})", serviceName);
            serviceStatus.put(serviceName.toLowerCase(), "running");
            return "服务 " + serviceName + " 重启成功";
        }
    }

    public static void main(String[] args) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(TestApiConfig.BASE_URL)
                .apiKey(TestApiConfig.API_KEY)
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(TestApiConfig.MODEL)
                        .build())
                .build();

        // 会话记忆：支持多轮上下文
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();

        ChatClient agent = ChatClient.builder(chatModel)
                .defaultSystem("你是一个 Linux 运维 Agent。收到问题后先用工具查询状态，发现服务停止时主动重启并汇报结果。回答用中文，简洁。")
                .defaultTools(new OpsTools())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // 第一轮：Agent 应该先查状态，发现 mysql 停止后调用重启工具
        String round1 = agent.prompt().user("帮我看下 mysql 服务怎么样，有问题就处理掉").call().content();
        log.info("第一轮结果:{}", round1);

        // 第二轮：验证记忆，Agent 应记得上一轮处理过 mysql
        String round2 = agent.prompt().user("刚才你处理的是哪个服务？").call().content();
        log.info("第二轮结果:{}", round2);
    }
}
