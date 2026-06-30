package cn.wjagent.ai.api.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {

    private String agentId;
    private String userId;
    private String sessionId;
    private String message;

    /** 是否开启联网搜索 */
    private boolean webSearch;
}
