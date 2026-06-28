package cn.wjagent.ai.domain.agent.service.chat;

import cn.wjagent.ai.domain.agent.model.entity.ChatCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.wjagent.ai.domain.agent.service.IChatService;
import cn.wjagent.ai.domain.agent.service.IRagService;
import cn.wjagent.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.wjagent.ai.types.enums.ResponseCode;
import cn.wjagent.ai.types.exception.AppException;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatService implements IChatService {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @Resource
    private IRagService ragService;

    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    // sessionId -> Session 对象缓存，避免每次 blockingGet 查询
    private final Map<String, com.google.adk.sessions.Session> sessionCache = new ConcurrentHashMap<>();
    // direct 模式多轮对话历史：sessionId -> 消息列表
    private final Map<String, List<Message>> directSessionHistory = new ConcurrentHashMap<>();

    @Override
    public List<AiAgentConfigTableVO.Agent> queryAiAgentConfigList() {
        Map<String, AiAgentConfigTableVO> tables = aiAgentAutoConfigProperties.getTables();

        List<AiAgentConfigTableVO.Agent> agentList = new ArrayList<>();
        if (null != tables) {
            for (AiAgentConfigTableVO vo : tables.values()) {
                if (null != vo.getAgent()) {
                    agentList.add(vo.getAgent());
                }
            }
        }

        return agentList;
    }

    @Override
    public String createSession(String agentId, String userId) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        // direct 模式不使用 ADK runner，每次都创建新的独立 session
        if (aiAgentRegisterVO.isDirectMode()) {
            return java.util.UUID.randomUUID().toString();
        }

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        // 每次都创建新 session，保证不同对话记录独立
        Session session = runner.sessionService().createSession(appName, userId).blockingGet();
        return session.id();
    }

    @Override
    public List<String> handleMessage(String agentId, String userId, String message) {

        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        String sessionId = createSession(agentId, userId);

        return handleMessage(agentId, userId, sessionId, message);
    }

    @Override
    public List<String> handleMessage(String agentId, String userId, String sessionId, String message) {

        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        Content userMsg = Content.fromParts(Part.fromText(message));

        Session session = resolveSession(runner, appName, agentId, userId, sessionId);
        Flowable<Event> events = runner.runAsync(session, userMsg, RunConfig.builder().build());

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        return outputs;
    }

    @Override
    public Flowable<Event> handleMessageStream(String agentId, String userId, String sessionId, String message) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        // RAG：检索相关知识片段并注入到用户消息前
        String enrichedMessage = enrichWithRagContext(agentId, message);
        Content userMsg = Content.fromParts(Part.fromText(enrichedMessage));

        Session session = resolveSession(runner, appName, agentId, userId, sessionId);
        return runner.runAsync(session, userMsg, RunConfig.builder().build());
    }

    private String enrichWithRagContext(String agentId, String message) {
        try {
            List<String> contexts = ragService.search(agentId, message, 3);
            if (contexts.isEmpty()) return message;
            StringBuilder sb = new StringBuilder();
            sb.append("【参考知识库】\n");
            for (int i = 0; i < contexts.size(); i++) {
                sb.append(i + 1).append(". ").append(contexts.get(i)).append("\n");
            }
            sb.append("\n【用户问题】\n").append(message);
            return sb.toString();
        } catch (Exception e) {
            log.warn("RAG 检索失败，忽略上下文注入", e);
            return message;
        }
    }

    @Override
    public Flowable<Event> handleMessageStream(ChatCommandEntity chatCommandEntity) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(chatCommandEntity.getAgentId());
        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        List<Part> parts = buildParts(chatCommandEntity);
        Content content = Content.builder().role("user").parts(parts).build();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = resolveSession(runner, aiAgentRegisterVO.getAppName(),
                chatCommandEntity.getAgentId(), chatCommandEntity.getUserId(), chatCommandEntity.getSessionId());
        return runner.runAsync(session, content, RunConfig.builder().build());
    }

    @Override
    public List<String> handleMessage(ChatCommandEntity chatCommandEntity) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(chatCommandEntity.getAgentId());

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        List<Part> parts = buildParts(chatCommandEntity);
        Content content = Content.builder().role("user").parts(parts).build();

        // 获取运行体
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Flowable<Event> events = runner.runAsync(chatCommandEntity.getUserId(), chatCommandEntity.getSessionId(), content);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        return outputs;
    }

    private List<Part> buildParts(ChatCommandEntity entity) {
        List<Part> parts = new ArrayList<>();
        if (entity.getTexts() != null) {
            for (ChatCommandEntity.Content.Text t : entity.getTexts()) {
                parts.add(Part.fromText(t.getMessage()));
            }
        }
        if (entity.getFiles() != null) {
            for (ChatCommandEntity.Content.File f : entity.getFiles()) {
                parts.add(Part.fromUri(f.getFileUri(), f.getMimeType()));
            }
        }
        if (entity.getInlineDatas() != null) {
            for (ChatCommandEntity.Content.InlineData d : entity.getInlineDatas()) {
                parts.add(Part.fromBytes(d.getBytes(), d.getMimeType()));
            }
        }
        return parts;
    }

    @Override
    public boolean isDirectMode(String agentId) {
        AiAgentRegisterVO vo = defaultArmoryFactory.getAiAgentRegisterVO(agentId);
        return vo != null && vo.isDirectMode();
    }

    @Override
    public Flowable<String> handleMessageStreamDirect(String agentId, String userId, String sessionId, String message) {
        AiAgentRegisterVO vo = defaultArmoryFactory.getAiAgentRegisterVO(agentId);
        if (vo == null || !vo.isDirectMode()) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        String enrichedMessage = enrichWithRagContext(agentId, message);
        String historyKey = agentId + ":" + (sessionId != null ? sessionId : userId);
        List<Message> history = directSessionHistory.computeIfAbsent(historyKey, k -> new ArrayList<>());

        List<Message> messages = new ArrayList<>();
        if (vo.getSystemPrompt() != null && !vo.getSystemPrompt().isBlank()) {
            messages.add(new SystemMessage(vo.getSystemPrompt()));
        }
        messages.addAll(history);
        messages.add(new UserMessage(enrichedMessage));

        Prompt prompt = new Prompt(messages);

        // 优先使用用户个人配置的模型，没有则退回全局配置
        org.springframework.ai.chat.model.ChatModel chatModel =
                defaultArmoryFactory.getUserChatModel(userId, agentId);
        if (chatModel == null) chatModel = vo.getChatModel();
        final org.springframework.ai.chat.model.ChatModel finalChatModel = chatModel;

        return Flowable.create(emitter -> {
            StringBuilder fullResponse = new StringBuilder();
            try {
                finalChatModel.stream(prompt).subscribe(
                        resp -> {
                            try {
                                String text = (resp.getResult() != null && resp.getResult().getOutput() != null)
                                        ? resp.getResult().getOutput().getText() : null;
                                if (text != null && !text.isEmpty()) {
                                    fullResponse.append(text);
                                    emitter.onNext(text);
                                }
                            } catch (Exception ex) {
                                emitter.onError(ex);
                            }
                        },
                        emitter::onError,
                        () -> {
                            history.add(new UserMessage(enrichedMessage));
                            history.add(new AssistantMessage(fullResponse.toString()));
                            // 控制历史长度，最多保留 20 轮（40条消息）
                            while (history.size() > 40) {
                                history.remove(0);
                            }
                            emitter.onComplete();
                        }
                );
            } catch (Exception e) {
                emitter.onError(e);
            }
        }, io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER);
    }

    /**
     * Look up session by sessionId; if missing (e.g. server restart), create a new one and
     * update userSessions so subsequent calls for this (agentId, userId) use the new id.
     */
    private Session resolveSession(InMemoryRunner runner, String appName,
                                   String agentId, String userId, String sessionId) {
        // 先查缓存，避免每次 blockingGet
        if (sessionId != null && !sessionId.isEmpty()) {
            Session cached = sessionCache.get(sessionId);
            if (cached != null) return cached;
        }
        Session session = runner.sessionService()
                .getSession(appName, userId, sessionId, java.util.Optional.empty())
                .blockingGet();
        if (session == null) {
            session = runner.sessionService().createSession(appName, userId).blockingGet();
            userSessions.put(agentId + ":" + userId, session.id());
        }
        sessionCache.put(session.id(), session);
        return session;
    }

}
