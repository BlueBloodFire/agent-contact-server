package cn.wjagent.ai.domain.agent.service.armory.factory.matter.mcp.client.impl;

import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.service.armory.factory.matter.mcp.client.ToolMcpCreateService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service("localToolMcpCreateService")
public class LocalToolMcpCreateService implements ToolMcpCreateService {

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) {
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.LocalParameters local = toolMcp.getLocal();
        Object localBean = applicationContext.getBean(local.getName());
        log.info("Tool Local MCP Initialized {}", local.getName());
        return MethodToolCallbackProvider.builder().toolObjects(localBean).build().getToolCallbacks();
    }

}