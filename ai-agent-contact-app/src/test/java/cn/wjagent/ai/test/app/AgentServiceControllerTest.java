package cn.wjagent.ai.test.app;

import cn.wjagent.ai.api.dto.ChatRequestDTO;
import cn.wjagent.ai.api.dto.CreateSessionRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AgentServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试查询智能体配置列表接口
     * GET /api/v1/query_ai_agent_config_list
     * 不依赖 AI 服务，只读 YAML 配置
     */
    @Test
    public void test_queryAiAgentConfigList() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/query_ai_agent_config_list"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        log.info("查询智能体配置列表响应: {}", content);

        Assert.assertNotNull("响应不应为空", content);
        Assert.assertTrue("响应应包含成功码", content.contains("0000"));
    }

    /**
     * 测试创建会话接口
     * POST /api/v1/create_session
     */
    @Test
    public void test_createSession() throws Exception {
        CreateSessionRequestDTO requestDTO = new CreateSessionRequestDTO();
        requestDTO.setAgentId("100001");
        requestDTO.setUserId("test-user-001");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/create_session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        log.info("创建会话响应: {}", content);

        Assert.assertNotNull("响应不应为空", content);
        Assert.assertTrue("响应应包含成功码", content.contains("0000"));
        Assert.assertTrue("响应应包含 sessionId", content.contains("sessionId"));
    }

    /**
     * 测试创建会话接口 - Agent 不存在时返回错误码
     */
    @Test
    public void test_createSession_agentNotFound() throws Exception {
        CreateSessionRequestDTO requestDTO = new CreateSessionRequestDTO();
        requestDTO.setAgentId("999999");
        requestDTO.setUserId("test-user-001");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/create_session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        log.info("Agent 不存在时响应: {}", content);

        Assert.assertTrue("应返回 E0001 错误码", content.contains("E0001"));
    }

    /**
     * 测试阻塞式对话接口
     * POST /api/v1/chat
     */
    @Test
    public void test_chat() throws Exception {
        ChatRequestDTO requestDTO = new ChatRequestDTO();
        requestDTO.setAgentId("100001");
        requestDTO.setUserId("test-user-chat");
        requestDTO.setMessage("你好，用一句话介绍自己");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        log.info("对话响应: {}", content);

        Assert.assertNotNull("响应不应为空", content);
        Assert.assertTrue("响应应包含成功码", content.contains("0000"));
        Assert.assertTrue("响应应包含 content 字段", content.contains("content"));
    }

    /**
     * 测试流式对话接口
     * POST /api/v1/chat_stream
     */
    @Test
    public void test_chatStream() throws Exception {
        ChatRequestDTO requestDTO = new ChatRequestDTO();
        requestDTO.setAgentId("100001");
        requestDTO.setUserId("test-user-stream");
        requestDTO.setMessage("你好");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat_stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        log.info("流式对话响应: {}", content);

        Assert.assertFalse("流式对话应有输出", content.isEmpty());
    }

    /**
     * 测试对话接口 - 携带已有 sessionId 复用会话
     */
    @Test
    public void test_chat_withExistingSession() throws Exception {
        // 先创建 session
        CreateSessionRequestDTO sessionReq = new CreateSessionRequestDTO();
        sessionReq.setAgentId("100001");
        sessionReq.setUserId("test-user-session");

        MvcResult sessionResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/create_session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sessionReq)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String sessionContent = sessionResult.getResponse().getContentAsString();
        log.info("创建会话响应: {}", sessionContent);
        Assert.assertTrue("创建会话应成功", sessionContent.contains("0000"));

        // 从响应提取 sessionId
        String sessionId = objectMapper.readTree(sessionContent)
                .path("data").path("sessionId").asText();
        Assert.assertFalse("sessionId 不应为空", sessionId.isEmpty());

        // 用 sessionId 发起对话
        ChatRequestDTO chatReq = new ChatRequestDTO();
        chatReq.setAgentId("100001");
        chatReq.setUserId("test-user-session");
        chatReq.setSessionId(sessionId);
        chatReq.setMessage("记住我的名字：小明");

        MvcResult chatResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String chatContent = chatResult.getResponse().getContentAsString();
        log.info("携带 sessionId 的对话响应: {}", chatContent);
        Assert.assertTrue("对话应成功", chatContent.contains("0000"));
    }

}
