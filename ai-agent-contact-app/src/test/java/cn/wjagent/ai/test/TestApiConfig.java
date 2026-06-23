package cn.wjagent.ai.test;

/**
 * AI 测试共用配置
 * 所有 agent/model/tool 测试统一从这里取连接参数，换 key 只改这一处
 */
public class TestApiConfig {

    /** OpenAI 兼容接口地址 */
    public static final String BASE_URL = "https://api.deepseek.com";

    /** API Key（需要换成你的 apikey） */
    public static final String API_KEY = "sk-1e03bd81f3d241b4af9cd71e9a628ea8";

    /** 模型名 */
    public static final String MODEL = "deepseek-v4-flash";

    private TestApiConfig() {
    }
}
