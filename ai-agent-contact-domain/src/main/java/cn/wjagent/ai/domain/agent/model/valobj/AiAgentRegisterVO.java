package cn.wjagent.ai.domain.agent.model.valobj;


import com.google.adk.runner.InMemoryRunner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAgentRegisterVO {

    private String appName;

    private String agentId;

    private String agentName;

    private String agentDesc;

    private InMemoryRunner runner;

    /** Spring AI ChatModel，direct 模式下使用 */
    private ChatModel chatModel;

    /** direct 模式系统提示词 */
    private String systemPrompt;

    /** true 表示使用 Spring AI 直接流式，不走 ADK */
    private boolean directMode;
}
