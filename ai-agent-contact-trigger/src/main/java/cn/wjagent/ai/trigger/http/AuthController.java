package cn.wjagent.ai.trigger.http;

import cn.wjagent.ai.api.dto.LoginRequestDTO;
import cn.wjagent.ai.api.dto.LoginResponseDTO;
import cn.wjagent.ai.api.response.Response;
import cn.wjagent.ai.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Map<String, String> USERS = Map.of(
            "admin", "admin",
            "rootUser", "rootUser",
            "testUser", "testUser"
    );

    public static final Map<String, TokenInfo> TOKEN_STORE = new ConcurrentHashMap<>();

    private static final long TOKEN_TTL_MS = 8 * 60 * 60 * 1000L;

    public record TokenInfo(String username, long expireAt) {}

    @PostMapping("login")
    public Response<LoginResponseDTO> login(@RequestBody LoginRequestDTO req) {
        String expectedPwd = USERS.get(req.getUsername());
        if (expectedPwd == null || !expectedPwd.equals(req.getPassword())) {
            return Response.<LoginResponseDTO>builder()
                    .code("E_AUTH")
                    .info("用户名或密码错误")
                    .build();
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + TOKEN_TTL_MS;
        TOKEN_STORE.put(token, new TokenInfo(req.getUsername(), expireAt));

        LoginResponseDTO dto = new LoginResponseDTO();
        dto.setToken(token);
        dto.setUsername(req.getUsername());
        dto.setExpireAt(expireAt);

        log.info("用户登录成功 username:{}", req.getUsername());
        return Response.<LoginResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(dto)
                .build();
    }

    @PostMapping("logout")
    public Response<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            TOKEN_STORE.remove(token);
        }
        return Response.<Void>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .build();
    }
}
