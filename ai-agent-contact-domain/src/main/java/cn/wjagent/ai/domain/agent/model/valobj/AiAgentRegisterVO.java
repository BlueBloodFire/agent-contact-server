package cn.wjagent.ai.domain.agent.model.valobj;


import com.google.adk.runner.InMemoryRunner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
