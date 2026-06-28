package cn.wjagent.ai.trigger.http;

import cn.wjagent.ai.api.IAgentService;
import cn.wjagent.ai.api.dto.*;
import cn.wjagent.ai.api.response.Response;
import cn.wjagent.ai.domain.agent.model.entity.ChatCommandEntity;
import cn.wjagent.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.wjagent.ai.domain.agent.service.IChatService;
import cn.wjagent.ai.types.enums.ResponseCode;
import cn.wjagent.ai.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@CrossOrigin(origins = "*")
public class AgentServiceController implements IAgentService {

    private static final long STREAM_IDLE_TIMEOUT_MS = 90_000L;
    private static final long STALL_THRESHOLD_MS = 30_000L;
    private static final ScheduledExecutorService watchdog =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "stream-watchdog");
                t.setDaemon(true);
                return t;
            });

    @Resource
    private IChatService chatService;

    @RequestMapping(value = "query_ai_agent_config_list", method = RequestMethod.GET)
    @Override
    public Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList() {
        try {
            log.info("查询智能体配置列表");
            List<AiAgentConfigTableVO.Agent> agentConfigs = chatService.queryAiAgentConfigList();

            List<AiAgentConfigResponseDTO> responseDTOS = agentConfigs.stream().map(agentConfig -> {
                AiAgentConfigResponseDTO responseDTO = new AiAgentConfigResponseDTO();
                responseDTO.setAgentId(agentConfig.getAgentId());
                responseDTO.setAgentName(agentConfig.getAgentName());
                responseDTO.setAgentDesc(agentConfig.getAgentDesc());
                return responseDTO;
            }).collect(Collectors.toList());

            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTOS)
                    .build();
        } catch (AppException e) {
            log.error("查询智能体配置列表异常", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询智能体配置列表失败", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    // fix: GET -> POST，@RequestBody 在 GET 请求中不生效
    @RequestMapping(value = "create_session", method = RequestMethod.POST)
    @Override
    public Response<CreateSessionResponseDTO> createSession(@RequestBody CreateSessionRequestDTO requestDTO) {
        try {
            log.info("创建会话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            String sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());

            CreateSessionResponseDTO responseDTO = new CreateSessionResponseDTO();
            responseDTO.setSessionId(sessionId);

            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("创建会话异常", e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("创建会话失败 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "chat", method = RequestMethod.POST)
    @Override
    public Response<ChatResponseDTO> chat(@RequestBody ChatRequestDTO requestDTO) {
        try {
            log.info("智能体对话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            }

            List<String> messages = chatService.handleMessage(requestDTO.getAgentId(), requestDTO.getUserId(), sessionId, requestDTO.getMessage());

            ChatResponseDTO responseDTO = new ChatResponseDTO();
            responseDTO.setContent(String.join("\n", messages));

            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("智能体对话异常", e);
            return Response.<ChatResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("智能体对话失败 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @PostMapping(value = "chat_multimodal_stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseBodyEmitter chatMultimodalStream(
            @RequestParam String agentId,
            @RequestParam String userId,
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestParam(required = false) List<MultipartFile> files) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(3 * 60 * 1000L);
        try {
            log.info("多模态流式对话 agentId:{} userId:{} sessionId:{} files:{}", agentId, userId, sessionId,
                    files == null ? 0 : files.size());

            String resolvedSessionId = (sessionId == null || sessionId.isEmpty())
                    ? chatService.createSession(agentId, userId) : sessionId;

            List<ChatCommandEntity.Content.Text> texts = new ArrayList<>();
            texts.add(new ChatCommandEntity.Content.Text(message));

            List<ChatCommandEntity.Content.InlineData> inlineDatas = new ArrayList<>();
            if (files != null) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        inlineDatas.add(new ChatCommandEntity.Content.InlineData(
                                file.getBytes(), file.getContentType()));
                    }
                }
            }

            ChatCommandEntity entity = ChatCommandEntity.builder()
                    .agentId(agentId)
                    .userId(userId)
                    .sessionId(resolvedSessionId)
                    .texts(texts)
                    .inlineDatas(inlineDatas.isEmpty() ? null : inlineDatas)
                    .build();

            long startMs = System.currentTimeMillis();
            AtomicBoolean firstChunk = new AtomicBoolean(true);
            AtomicLong lastEventMs = new AtomicLong(startMs);
            AtomicBoolean completed = new AtomicBoolean(false);

            ScheduledFuture<?>[] watchdogTask = new ScheduledFuture<?>[1];
            watchdogTask[0] = watchdog.scheduleAtFixedRate(() -> {
                if (completed.get()) return;
                long gap = System.currentTimeMillis() - lastEventMs.get();
                if (gap >= STREAM_IDLE_TIMEOUT_MS) {
                    log.error("多模态流式超时 agentId:{} 已等待{}ms", agentId, gap);
                    completed.set(true);
                    watchdogTask[0].cancel(false);
                    try { emitter.send("[TIMEOUT]", MediaType.TEXT_PLAIN); emitter.complete(); } catch (Exception ignored) {}
                }
            }, 10, 10, TimeUnit.SECONDS);

            chatService.handleMessageStream(entity)
                    .subscribe(
                            event -> {
                                try {
                                    String content = event.stringifyContent();
                                    if (content != null && !content.isEmpty()) {
                                        if (firstChunk.compareAndSet(true, false)) {
                                            log.info("多模态流式首次响应(TTFT) agentId:{} 耗时{}ms", agentId, System.currentTimeMillis() - startMs);
                                        }
                                        lastEventMs.set(System.currentTimeMillis());
                                        emitter.send(content, MediaType.TEXT_PLAIN);
                                    }
                                } catch (Exception e) {
                                    log.error("多模态流式发送失败", e);
                                    completed.set(true);
                                    watchdogTask[0].cancel(false);
                                    emitter.completeWithError(e);
                                }
                            },
                            err -> {
                                completed.set(true);
                                watchdogTask[0].cancel(false);
                                emitter.completeWithError(err);
                            },
                            () -> {
                                completed.set(true);
                                watchdogTask[0].cancel(false);
                                log.info("多模态流式完成 agentId:{} 总耗时{}ms", agentId, System.currentTimeMillis() - startMs);
                                emitter.complete();
                            }
                    );
        } catch (Exception e) {
            log.error("多模态流式对话失败", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @RequestMapping(value = "chat_stream", method = RequestMethod.POST,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Override
    public ResponseBodyEmitter chatStream(@RequestBody ChatRequestDTO requestDTO) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(3 * 60 * 1000L);
        try {
            long startMs = System.currentTimeMillis();
            log.info("流式对话 agentId:{} userId:{} sessionId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), requestDTO.getSessionId());
            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            }
            final String finalSessionId = sessionId;

            AtomicBoolean firstChunk = new AtomicBoolean(true);
            AtomicLong lastEventMs = new AtomicLong(startMs);
            AtomicBoolean completed = new AtomicBoolean(false);

            // 看门狗：90秒无数据则超时
            ScheduledFuture<?>[] watchdogTask = new ScheduledFuture<?>[1];
            watchdogTask[0] = watchdog.scheduleAtFixedRate(() -> {
                if (completed.get()) return;
                long gap = System.currentTimeMillis() - lastEventMs.get();
                if (gap >= STALL_THRESHOLD_MS && gap < STREAM_IDLE_TIMEOUT_MS) {
                    log.warn("流式响应疑似卡顿 agentId:{} 距上次事件{}ms", requestDTO.getAgentId(), gap);
                }
                if (gap >= STREAM_IDLE_TIMEOUT_MS) {
                    log.error("流式响应超时 agentId:{} 已等待{}ms，强制结束", requestDTO.getAgentId(), gap);
                    completed.set(true);
                    watchdogTask[0].cancel(false);
                    try {
                        emitter.send("[TIMEOUT]", MediaType.TEXT_PLAIN);
                        emitter.complete();
                    } catch (Exception ignored) {}
                }
            }, 10, 10, TimeUnit.SECONDS);

            if (chatService.isDirectMode(requestDTO.getAgentId())) {
                // direct 模式：Spring AI token 级流式
                chatService.handleMessageStreamDirect(requestDTO.getAgentId(), requestDTO.getUserId(), finalSessionId, requestDTO.getMessage())
                        .subscribe(
                                token -> {
                                    try {
                                        if (firstChunk.compareAndSet(true, false)) {
                                            log.info("流式首次响应(TTFT)[direct] agentId:{} 耗时{}ms", requestDTO.getAgentId(), System.currentTimeMillis() - startMs);
                                        }
                                        lastEventMs.set(System.currentTimeMillis());
                                        emitter.send(token, MediaType.TEXT_PLAIN);
                                    } catch (Exception e) {
                                        log.error("流式发送失败[direct]", e);
                                        completed.set(true);
                                        watchdogTask[0].cancel(false);
                                        emitter.completeWithError(e);
                                    }
                                },
                                err -> {
                                    completed.set(true);
                                    watchdogTask[0].cancel(false);
                                    log.error("流式对话错误[direct] agentId:{}", requestDTO.getAgentId(), err);
                                    emitter.completeWithError(err);
                                },
                                () -> {
                                    completed.set(true);
                                    watchdogTask[0].cancel(false);
                                    log.info("流式对话完成[direct] agentId:{} 总耗时{}ms", requestDTO.getAgentId(), System.currentTimeMillis() - startMs);
                                    emitter.complete();
                                }
                        );
            } else {
                chatService.handleMessageStream(requestDTO.getAgentId(), requestDTO.getUserId(), finalSessionId, requestDTO.getMessage())
                        .subscribe(
                                event -> {
                                    try {
                                        String content = event.stringifyContent();
                                        // 过滤空内容和JSON格式的工具调用/响应事件
                                        if (content != null && !content.isEmpty()) {
                                            String trimmed = content.trim();
                                            if (trimmed.startsWith("{") || trimmed.startsWith("[")) return;
                                            if (firstChunk.compareAndSet(true, false)) {
                                                log.info("流式首次响应(TTFT) agentId:{} 耗时{}ms", requestDTO.getAgentId(), System.currentTimeMillis() - startMs);
                                            }
                                            lastEventMs.set(System.currentTimeMillis());
                                            emitter.send(content, MediaType.TEXT_PLAIN);
                                        }
                                    } catch (Exception e) {
                                        log.error("流式对话发送失败", e);
                                        completed.set(true);
                                        watchdogTask[0].cancel(false);
                                        emitter.completeWithError(e);
                                    }
                                },
                                err -> {
                                    completed.set(true);
                                    watchdogTask[0].cancel(false);
                                    log.error("流式对话错误 agentId:{}", requestDTO.getAgentId(), err);
                                    emitter.completeWithError(err);
                                },
                                () -> {
                                    completed.set(true);
                                    watchdogTask[0].cancel(false);
                                    log.info("流式对话完成 agentId:{} 总耗时{}ms", requestDTO.getAgentId(), System.currentTimeMillis() - startMs);
                                    emitter.complete();
                                }
                        );
            }
        } catch (Exception e) {
            log.error("流式对话失败", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

}
