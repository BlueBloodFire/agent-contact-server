package cn.wjagent.ai.infrastructure.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessagePO {

    private Long id;
    private String messageId;
    private String sessionId;
    private String agentId;
    private String userId;
    /** user / assistant / system */
    private String role;
    private String content;
    private Integer tokenCount;
    private Date createdAt;
}
