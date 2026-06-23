package cn.wjagent.ai.domain.agent.service.armory.factory.matter.plugin;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.InvocationContext;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.plugins.BasePlugin;
import com.google.genai.types.Content;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("myTestPlugin")
public class MyTestPlugin extends BasePlugin {

    public MyTestPlugin() {
        super("MyTestPlugin");
    }

    @Override
    public Maybe<Content> onUserMessageCallback(InvocationContext invocationContext, Content userMessage) {
        log.info("用户输入信息:{}", userMessage.text());
        return Maybe.empty();
    }

    @Override
    public Maybe<Content> beforeAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        log.info("智能体名称:{}", agent.name());
        return Maybe.empty();
    }

    @Override
    public Maybe<LlmResponse> beforeModelCallback(CallbackContext callbackContext, LlmRequest llmRequest) {
        log.info("ai 模型:{}", llmRequest.model().orElse("unknown"));
        return Maybe.empty();
    }

}
