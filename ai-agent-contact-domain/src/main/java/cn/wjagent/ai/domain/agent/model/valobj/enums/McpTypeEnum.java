package cn.wjagent.ai.domain.agent.model.valobj.enums;

import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum McpTypeEnum {

    LOCAL("localToolMcpCreateService"),
    SSE("sseToolMcpCreateService"),
    STDIO("stdioToolMcpCreateService");

    private final String beanName;

    public static McpTypeEnum fromToolMcp(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) {
        if (toolMcp.getLocal() != null) return LOCAL;
        if (toolMcp.getSse() != null) return SSE;
        if (toolMcp.getStdio() != null) return STDIO;
        throw new RuntimeException("No MCP type matched for toolMcp config");
    }
}
