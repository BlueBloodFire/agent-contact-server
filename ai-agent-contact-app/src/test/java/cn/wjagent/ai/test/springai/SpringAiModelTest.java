package cn.wjagent.ai.test.springai;

import cn.wjagent.ai.test.TestApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Spring AI — Model 测试
 * 直连 OpenAI 兼容接口，发送一条消息验证模型连通性
 */
@Slf4j
public class SpringAiModelTest {

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

        String call = chatModel.call("hi 你好哇!");

        log.info("测试结果:{}", call);
    }
}
