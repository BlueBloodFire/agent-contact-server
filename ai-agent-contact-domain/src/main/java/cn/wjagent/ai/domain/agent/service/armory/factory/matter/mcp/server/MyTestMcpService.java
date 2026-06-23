package cn.wjagent.ai.domain.agent.service.armory.factory.matter.mcp.server;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component("myTestMcpService")
public class MyTestMcpService {

    @Tool(description = "获取当前系统时间")
    public String getCurrentTime() {
        return "当前时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "将两个整数相加并返回结果")
    public String add(int a, int b) {
        return String.valueOf(a + b);
    }

}
