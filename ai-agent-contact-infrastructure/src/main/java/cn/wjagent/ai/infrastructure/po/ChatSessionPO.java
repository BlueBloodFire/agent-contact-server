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
public class ChatSessionPO {

    private Long id;
    private String sessionId;
    private String agentId;
    private String userId;
    private String title;
    private Integer turnCount;
    private Date createdAt;
    private Date updatedAt;
}
