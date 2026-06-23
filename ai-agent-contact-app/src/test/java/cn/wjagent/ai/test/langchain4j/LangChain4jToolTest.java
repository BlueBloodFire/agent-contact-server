package cn.wjagent.ai.test.langchain4j;

import cn.wjagent.ai.test.TestApiConfig;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LangChain4j — Tool 测试
 * @Tool 注解定义工具 + AiServices 动态代理，模型自主决定调用
 */
@Slf4j
public class LangChain4jToolTest {

    /** 工具定义 */
    static class DateTimeTools {

        @Tool("获取当前日期时间，格式 yyyy-MM-dd HH:mm:ss")
        public String getCurrentDateTime() {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("[Tool 被调用] getCurrentDateTime -> {}", now);
            return now;
        }

        @Tool("计算两个整数相加的结果")
        public int add(@P("第一个加数") int a, @P("第二个加数") int b) {
            log.info("[Tool 被调用] add({}, {})", a, b);
            return a + b;
        }
    }

    /** AiServices 代理接口 */
    interface Assistant {
        String chat(String message);
    }

    public static void main(String[] args) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(TestApiConfig.BASE_URL + "/v1")
                .apiKey(TestApiConfig.API_KEY)
                .modelName(TestApiConfig.MODEL)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(new DateTimeTools())
                .build();

        String answer = assistant.chat("现在几点了？另外帮我算一下 35 + 47 等于多少");

        log.info("测试结果:{}", answer);
    }
}
