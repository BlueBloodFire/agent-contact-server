package cn.wjagent.ai.test.adk;

import cn.wjagent.ai.test.TestApiConfig;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.Map;

/**
 * Google ADK — Agent 测试
 * 完整 Agent：系统指令 + 多工具 + 同一 Session 多轮对话（带上下文记忆）
 * 验证 ReAct 式自主决策：先查状态 → 发现异常 → 调用重启 → 汇报
 */
@Slf4j
public class AdkAgentTest {

    /** Agent 可用的运维工具（有状态 mock：重启后状态变为 running） */
    public static class OpsTools {

        private final java.util.Map<String, String> serviceStatus = new java.util.HashMap<>(
                java.util.Map.of("nginx", "running", "mysql", "stopped"));

        @Schema(description = "查询指定服务的运行状态，返回 running/stopped")
        public Map<String, Object> getServiceStatus(
                @Schema(name = "serviceName", description = "服务名，如 nginx、mysql") String serviceName) {
            String status = serviceStatus.getOrDefault(serviceName.toLowerCase(), "stopped");
            log.info("[Tool 被调用] getServiceStatus({}) -> {}", serviceName, status);
            return Map.of("serviceName", serviceName, "status", status);
        }

        @Schema(description = "重启指定服务，返回操作结果")
        public Map<String, Object> restartService(
                @Schema(name = "serviceName", description = "服务名") String serviceName) {
            log.info("[Tool 被调用] restartService({})", serviceName);
            serviceStatus.put(serviceName.toLowerCase(), "running");
            return Map.of("serviceName", serviceName, "result", "重启成功");
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

        OpsTools tools = new OpsTools();

        LlmAgent agent = LlmAgent.builder()
                .name("ops-agent")
                .description("Linux 运维 Agent")
                .model(new com.google.adk.models.springai.SpringAI(chatModel))
                .instruction("你是一个 Linux 运维 Agent。收到问题后先用工具查询状态，发现服务停止时主动重启并汇报结果。回答用中文，简洁。")
                .tools(FunctionTool.create(tools, "getServiceStatus"),
                       FunctionTool.create(tools, "restartService"))
                .build();

        InMemoryRunner runner = new InMemoryRunner(agent);
        Session session = runner.sessionService()
                .createSession(runner.appName(), "test-user")
                .blockingGet();

        // 第一轮：Agent 应该先查状态，发现 mysql 停止后调用重启工具
        chat(runner, session, "帮我看下 mysql 服务怎么样，有问题就处理掉", "第一轮结果");

        // 第二轮：同一 Session，验证 Agent 记得上一轮处理过 mysql
        chat(runner, session, "刚才你处理的是哪个服务？", "第二轮结果");
    }

    private static void chat(InMemoryRunner runner, Session session, String message, String tag) {
        Flowable<Event> events = runner.runAsync(
                "test-user", session.id(),
                Content.fromParts(Part.fromText(message)));

        events.blockingForEach(event -> {
            if (event.finalResponse()) {
                log.info("{}:{}", tag, event.stringifyContent());
            }
        });
    }
}
