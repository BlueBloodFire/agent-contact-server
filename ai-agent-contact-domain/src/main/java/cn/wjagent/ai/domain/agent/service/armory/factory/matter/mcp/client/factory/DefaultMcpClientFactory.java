package cn.wjagent.ai.domain.agent.service.armory.factory.matter.mcp.client.factory;

import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.model.valobj.enums.McpTypeEnum;
import cn.wjagent.ai.domain.agent.service.armory.factory.matter.mcp.client.ToolMcpCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultMcpClientFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ToolMcpCreateService getToolMcpCreateService(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) {
        McpTypeEnum mcpTypeEnum = McpTypeEnum.fromToolMcp(toolMcp);
        return (ToolMcpCreateService) applicationContext.getBean(mcpTypeEnum.getBeanName());
    }

}