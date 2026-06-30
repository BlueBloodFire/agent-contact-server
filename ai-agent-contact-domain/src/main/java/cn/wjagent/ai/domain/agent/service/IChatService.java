package cn.wjagent.ai.domain.agent.service;

import cn.wjagent.ai.domain.agent.model.entity.ChatCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.google.adk.events.Event;
import io.reactivex.rxjava3.core.Flowable;

import java.util.List;

public interface IChatService {

    List<AiAgentConfigTableVO.Agent> queryAiAgentConfigList();

    String createSession(String agentId, String userId);

    List<String> handleMessage(String agentId, String userId, String message);

    List<String> handleMessage(String agentId, String userId, String sessionId, String message);

    Flowable<Event> handleMessageStream(String agentId, String userId, String sessionId, String message);

    List<String> handleMessage(ChatCommandEntity chatCommandEntity);

    Flowable<Event> handleMessageStream(ChatCommandEntity chatCommandEntity);

    /** 判断指定 agentId 是否为 direct 模式 */
    boolean isDirectMode(String agentId);

    /** direct 模式下的流式对话，返回 token 字符串流 */
    Flowable<String> handleMessageStreamDirect(String agentId, String userId, String sessionId, String message, boolean webSearch);

}
