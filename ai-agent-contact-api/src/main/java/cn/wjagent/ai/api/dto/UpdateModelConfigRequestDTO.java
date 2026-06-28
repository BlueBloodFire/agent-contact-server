package cn.wjagent.ai.api.dto;

import lombok.Data;

@Data
public class UpdateModelConfigRequestDTO {
    private String userId;
    private String agentId;
    private String baseUrl;
    private String apiKey;
    private String model;
}
