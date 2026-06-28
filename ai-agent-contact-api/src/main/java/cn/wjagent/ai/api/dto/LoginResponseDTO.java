package cn.wjagent.ai.api.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token;
    private String username;
    private long expireAt; // epoch millis
}
