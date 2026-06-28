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
public class AgentModelConfigPO {

    private Long id;
    private String userId;
    private String agentId;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Date updatedAt;
}
