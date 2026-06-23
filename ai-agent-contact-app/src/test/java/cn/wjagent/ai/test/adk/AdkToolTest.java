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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Google ADK — Tool 测试
 * 使用 @Schema 注解 + FunctionTool.create() 注册工具，模型自主决定调用
 */
@Slf4j
public class AdkToolTest {

    /** 工具定义：方法必须 public，参数用 @Schema 描述 */
    public static class DateTimeTools {

        @Schema(description = "获取当前日期时间，格式 yyyy-MM-dd HH:mm:ss")
        public Map<String, Object> getCurrentDateTime() {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("[Tool 被调用] getCurrentDateTime -> {}", now);
            return Map.of("dateTime", now);
        }

        @Schema(description = "计算两个整数相加的结果")
        public Map<String, Object> add(
                @Schema(name = "a", description = "第一个加数") int a,
                @Schema(name = "b", description = "第二个加数") int b) {
            log.info("[Tool 被调用] add({}, {})", a, b);
            return Map.of("result", a + b);
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

        DateTimeTools tools = new DateTimeTools();
        FunctionTool timeTool = FunctionTool.create(tools, "getCurrentDateTime");
        FunctionTool addTool = FunctionTool.create(tools, "add");

        LlmAgent agent = LlmAgent.builder()
                .name("tool-test-agent")
                .description("工具调用验证 Agent")
                .model(new com.google.adk.models.springai.SpringAI(chatModel))
                .instruction("你可以使用工具获取时间和做加法。需要时主动调用工具，用中文回答。")
                .tools(timeTool, addTool)
                .build();

        InMemoryRunner runner = new InMemoryRunner(agent);
        Session session = runner.sessionService()
                .createSession(runner.appName(), "test-user")
                .blockingGet();

        Flowable<Event> events = runner.runAsync(
                "test-user", session.id(),
                Content.fromParts(Part.fromText("现在几点了？另外帮我算一下 35 + 47 等于多少")));

        events.blockingForEach(event -> {
            if (event.finalResponse()) {
                log.info("测试结果:{}", event.stringifyContent());
            }
        });
    }
}
