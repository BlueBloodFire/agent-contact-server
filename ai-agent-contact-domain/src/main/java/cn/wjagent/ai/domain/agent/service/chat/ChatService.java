package cn.wjagent.ai.domain.agent.service.chat;

import cn.wjagent.ai.domain.agent.adapter.repository.IChatHistoryRepository;
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
import cn.wjagent.ai.domain.agent.service.armory.factory.matter.tool.BingSearchTool;
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

    @Resource
    private IChatHistoryRepository chatHistoryRepository;

    @Resource
    private BingSearchTool bingSearchTool;

    private static final int TOKEN_BUDGET = 6000;

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
            String sessionId = java.util.UUID.randomUUID().toString();
            chatHistoryRepository.saveSession(sessionId, agentId, userId, "");
            return sessionId;
        }

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        // 每次都创建新 session，保证不同对话记录独立
        Session session = runner.sessionService().createSession(appName, userId).blockingGet();
        chatHistoryRepository.saveSession(session.id(), agentId, userId, "");
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

    /** Returns a RAG SystemMessage or null if no context found. */
    private SystemMessage buildRagSystemMessage(String agentId, String query) {
        try {
            List<String> contexts = ragService.search(agentId, query, 3);
            if (contexts.isEmpty()) return null;
            StringBuilder sb = new StringBuilder("【参考知识库】\n");
            for (int i = 0; i < contexts.size(); i++) {
                sb.append(i + 1).append(". ").append(contexts.get(i)).append("\n");
            }
            return new SystemMessage(sb.toString());
        } catch (Exception e) {
            log.warn("RAG 检索失败，忽略上下文注入", e);
            return null;
        }
    }

    private String enrichWithRagContext(String agentId, String message) {
        SystemMessage rag = buildRagSystemMessage(agentId, message);
        if (rag == null) return message;
        return rag.getText() + "\n【用户问题】\n" + message;
    }

    /** Trim history to fit within TOKEN_BUDGET, keeping the most recent messages. */
    private List<Message> trimToTokenBudget(List<Message> history) {
        int total = 0;
        int cutFrom = history.size();
        for (int i = history.size() - 1; i >= 0; i--) {
            total += history.get(i).getText().length() / 4;
            if (total > TOKEN_BUDGET) break;
            cutFrom = i;
        }
        return history.subList(cutFrom, history.size());
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
    public Flowable<String> handleMessageStreamDirect(String agentId, String userId, String sessionId, String message, boolean webSearch) {
        AiAgentRegisterVO vo = defaultArmoryFactory.getAiAgentRegisterVO(agentId);
        if (vo == null || !vo.isDirectMode()) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        String historyKey = agentId + ":" + (sessionId != null ? sessionId : userId);
        List<Message> history = directSessionHistory.computeIfAbsent(historyKey, k -> {
            // restore from DB on cache miss (e.g. after restart)
            List<Message> restored = new ArrayList<>();
            for (IChatHistoryRepository.MessageRecord r : chatHistoryRepository.loadMessages(sessionId != null ? sessionId : userId)) {
                if ("user".equals(r.role)) restored.add(new UserMessage(r.content));
                else if ("assistant".equals(r.role)) restored.add(new AssistantMessage(r.content));
            }
            return restored;
        });

        int currentTurn = history.size() / 2 + 1;
        List<Message> messages = new ArrayList<>();
        String basePrompt = (vo.getSystemPrompt() != null && !vo.getSystemPrompt().isBlank())
                ? vo.getSystemPrompt() : "";
        String webSearchNote = webSearch
                ? "\n\n【重要】用户已开启联网搜索，系统会在用户问题后附上实时搜索结果。你必须优先基于搜索结果回答，不要说自己无法获取实时信息。如果搜索结果中有相关内容，直接给出答案；如果搜索结果不足以回答，再结合自身知识补充。"
                : "";
        String contextNote = "\n[当前对话轮次: " + currentTurn + "]";
        messages.add(new SystemMessage(basePrompt + webSearchNote + contextNote));
        SystemMessage ragMsg = buildRagSystemMessage(agentId, message);
        if (ragMsg != null) {
            messages.add(ragMsg);
        }
        messages.addAll(trimToTokenBudget(history));

        // 联网搜索：将搜索结果拼入用户消息
        if (webSearch) {
            try {
                java.util.Map<String, Object> searchResult = bingSearchTool.search(message);
                Object results = searchResult.get("results");
                if (results != null && !results.toString().isEmpty()) {
                    String enriched = message + "\n\n【以下是系统为你检索到的实时网络信息，请基于此回答】\n" + results;
                    messages.add(new UserMessage(enriched));
                } else {
                    messages.add(new UserMessage(message));
                }
            } catch (Exception e) {
                log.warn("联网搜索失败，跳过注入 message:{}", message, e);
                messages.add(new UserMessage(message));
            }
        } else {
            messages.add(new UserMessage(message));
        }

        log.debug("发送历史消息数:{} historyKey:{} webSearch:{}", history.size(), historyKey, webSearch);

        Prompt prompt = new Prompt(messages);

        org.springframework.ai.chat.model.ChatModel chatModel =
                defaultArmoryFactory.getUserChatModel(userId, agentId);
        if (chatModel == null) chatModel = vo.getChatModel();
        final org.springframework.ai.chat.model.ChatModel finalChatModel = chatModel;
        final String effectiveSessionId = sessionId;

        StringBuilder fullResponse = new StringBuilder();

        // 使用 fromPublisher 正确桥接 Reactor Flux → RxJava Flowable，避免 Flowable.create 的竞态问题
        reactor.core.publisher.Flux<String> responseFlux = finalChatModel.stream(prompt)
                .map(resp -> (resp.getResult() != null && resp.getResult().getOutput() != null
                        && resp.getResult().getOutput().getText() != null)
                        ? resp.getResult().getOutput().getText() : "")
                .filter(text -> !text.isEmpty())
                .retry(1)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    log.debug("AI响应完成 sessionId:{} 响应长度:{}", effectiveSessionId, fullResponse.length());
                    String msgUserId = java.util.UUID.randomUUID().toString();
                    String msgAiId = java.util.UUID.randomUUID().toString();
                    chatHistoryRepository.saveMessage(msgUserId, effectiveSessionId, agentId, userId, "user", message);
                    chatHistoryRepository.saveMessage(msgAiId, effectiveSessionId, agentId, userId, "assistant", fullResponse.toString());
                    history.add(new UserMessage(message));
                    history.add(new AssistantMessage(fullResponse.toString()));
                    int turns = history.size() / 2;
                    chatHistoryRepository.updateSessionTurnCount(effectiveSessionId, turns);
                    if (turns == 1) {
                        String title = message.length() > 50 ? message.substring(0, 50) : message;
                        chatHistoryRepository.updateSessionTitle(effectiveSessionId, title);
                    }
                });

        return Flowable.fromPublisher(responseFlux);
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
