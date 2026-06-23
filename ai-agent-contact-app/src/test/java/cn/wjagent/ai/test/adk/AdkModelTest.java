package cn.wjagent.ai.test.adk;

import cn.wjagent.ai.test.TestApiConfig;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Google ADK — Model 测试
 * 通过 google-adk-spring-ai 桥接（new SpringAI(chatModel)）接入 OpenAI 兼容接口，
 * 构建无工具的最小 Agent 验证模型连通性
 */
@Slf4j
public class AdkModelTest {

    public static void main(String[] args) {
        // 1. Spring AI ChatModel（OpenAI 兼容接口）
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

        // 2. 桥接到 ADK：new SpringAI(chatModel)
        LlmAgent agent = LlmAgent.builder()
                .name("model-test-agent")
                .description("模型连通性验证 Agent")
                .model(new com.google.adk.models.springai.SpringAI(chatModel))
                .instruction("你是一个友好的助手，用中文简洁回答。")
                .build();

        // 3. Runner 执行
        InMemoryRunner runner = new InMemoryRunner(agent);
        Session session = runner.sessionService()
                .createSession(runner.appName(), "test-user")
                .blockingGet();

        Flowable<Event> events = runner.runAsync(
                "test-user", session.id(),
                Content.fromParts(Part.fromText("hi 你好哇!")));

        events.blockingForEach(event -> {
            if (event.finalResponse()) {
                log.info("测试结果:{}", event.stringifyContent());
            }
        });
    }
}
