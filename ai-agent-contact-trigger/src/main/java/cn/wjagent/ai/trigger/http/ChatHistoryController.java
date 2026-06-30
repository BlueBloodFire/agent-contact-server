package cn.wjagent.ai.trigger.http;

import cn.wjagent.ai.api.response.Response;
import cn.wjagent.ai.infrastructure.dao.ChatMessageDao;
import cn.wjagent.ai.infrastructure.dao.ChatSessionDao;
import cn.wjagent.ai.infrastructure.po.ChatMessagePO;
import cn.wjagent.ai.infrastructure.po.ChatSessionPO;
import cn.wjagent.ai.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@CrossOrigin(origins = "*")
public class ChatHistoryController {

    @Resource
    private ChatSessionDao chatSessionDao;

    @Resource
    private ChatMessageDao chatMessageDao;

    @GetMapping("sessions")
    public Response<List<SessionVO>> getSessions(
            @RequestParam String userId,
            @RequestParam(required = false) String agentId) {
        try {
            List<ChatSessionPO> sessions = agentId != null
                    ? chatSessionDao.findByUserIdAndAgentId(userId, agentId)
                    : chatSessionDao.findByUserId(userId);
            List<SessionVO> vos = sessions.stream().map(s -> {
                SessionVO vo = new SessionVO();
                vo.setSessionId(s.getSessionId());
                vo.setAgentId(s.getAgentId());
                vo.setTitle(s.getTitle());
                vo.setTurnCount(s.getTurnCount());
                vo.setCreatedAt(s.getCreatedAt());
                vo.setUpdatedAt(s.getUpdatedAt());
                return vo;
            }).collect(Collectors.toList());
            return Response.<List<SessionVO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(vos)
                    .build();
        } catch (Exception e) {
            log.error("getSessions failed userId:{}", userId, e);
            return Response.<List<SessionVO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    @GetMapping("session/messages")
    public Response<List<MessageVO>> getMessages(@RequestParam String sessionId) {
        try {
            List<ChatMessagePO> messages = chatMessageDao.findBySessionId(sessionId);
            List<MessageVO> vos = messages.stream().map(m -> {
                MessageVO vo = new MessageVO();
                vo.setMessageId(m.getMessageId());
                vo.setRole(m.getRole());
                vo.setContent(m.getContent());
                vo.setCreatedAt(m.getCreatedAt());
                return vo;
            }).collect(Collectors.toList());
            return Response.<List<MessageVO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(vos)
                    .build();
        } catch (Exception e) {
            log.error("getMessages failed sessionId:{}", sessionId, e);
            return Response.<List<MessageVO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    @Data
    public static class SessionVO {
        private String sessionId;
        private String agentId;
        private String title;
        private Integer turnCount;
        private Date createdAt;
        private Date updatedAt;
    }

    @Data
    public static class MessageVO {
        private String messageId;
        private String role;
        private String content;
        private Date createdAt;
    }
}
