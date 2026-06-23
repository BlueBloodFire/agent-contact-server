package cn.wjagent.ai.api;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import cn.wjagent.ai.api.dto.*;
import cn.wjagent.ai.api.response.Response;
import java.util.List;


public interface IAgentService {

    Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList();

    Response<CreateSessionResponseDTO> createSession(CreateSessionRequestDTO requestDTO);

    Response<ChatResponseDTO> chat(ChatRequestDTO requestDTO);

    ResponseBodyEmitter chatStream(ChatRequestDTO requestDTO);

}
