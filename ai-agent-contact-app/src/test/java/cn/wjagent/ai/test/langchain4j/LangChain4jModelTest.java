package cn.wjagent.ai.test.langchain4j;

import cn.wjagent.ai.test.TestApiConfig;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * LangChain4j — Model 测试
 * 直连 OpenAI 兼容接口，发送一条消息验证模型连通性
 */
@Slf4j
public class LangChain4jModelTest {

    public static void main(String[] args) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(TestApiConfig.BASE_URL + "/v1")
                .apiKey(TestApiConfig.API_KEY)
                .modelName(TestApiConfig.MODEL)
                .logRequests(true)
                .logResponses(true)
                .build();

        String answer = chatModel.chat("hi 你好哇!");

        log.info("测试结果:{}", answer);
    }
}
