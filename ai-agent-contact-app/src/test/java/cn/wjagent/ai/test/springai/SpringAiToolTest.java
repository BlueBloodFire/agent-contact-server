package cn.wjagent.ai.test.springai;

import cn.wjagent.ai.test.TestApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Spring AI — Tool 测试
 * 通过 @Tool 注解定义工具，模型自主决定是否调用（Function Calling）
 */
@Slf4j
public class SpringAiToolTest {

    /** 工具定义：@Tool 注解的方法会被注册为可调用函数 */
    static class DateTimeTools {

        @Tool(description = "获取当前日期时间，格式 yyyy-MM-dd HH:mm:ss")
        public String getCurrentDateTime() {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("[Tool 被调用] getCurrentDateTime -> {}", now);
            return now;
        }

        @Tool(description = "计算两个整数相加的结果")
        public int add(@ToolParam(description = "第一个加数") int a,
                       @ToolParam(description = "第二个加数") int b) {
            log.info("[Tool 被调用] add({}, {})", a, b);
            return a + b;
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

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String answer = chatClient.prompt()
                .user("现在几点了？另外帮我算一下 35 + 47 等于多少")
                .tools(new DateTimeTools())
                .call()
                .content();

        log.info("测试结果:{}", answer);
    }
}
