package cn.wjagent.ai.trigger.http;

import cn.wjagent.ai.api.dto.UpdateModelConfigRequestDTO;
import cn.wjagent.ai.api.response.Response;
import cn.wjagent.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.wjagent.ai.infrastructure.dao.AgentModelConfigDao;
import cn.wjagent.ai.infrastructure.po.AgentModelConfigPO;
import cn.wjagent.ai.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@CrossOrigin(origins = "*")
public class ModelConfigController {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Resource
    private AgentModelConfigDao agentModelConfigDao;

    @PostMapping("update_model_config")
    public Response<Void> updateModelConfig(@RequestBody UpdateModelConfigRequestDTO req) {
        try {
            log.info("更新模型配置 userId:{} agentId:{} baseUrl:{} model:{}",
                    req.getUserId(), req.getAgentId(), req.getBaseUrl(), req.getModel());

            // 重建全局 Agent（使新 key/model 立即生效）
            defaultArmoryFactory.updateModelConfig(
                    req.getAgentId(), req.getBaseUrl(), req.getApiKey(), req.getModel());

            // 同时缓存用户级 ChatModel
            defaultArmoryFactory.updateUserModelConfig(
                    req.getUserId(), req.getAgentId(), req.getBaseUrl(), req.getApiKey(), req.getModel());

            // 持久化
            agentModelConfigDao.upsert(AgentModelConfigPO.builder()
                    .userId(req.getUserId())
                    .agentId(req.getAgentId())
                    .baseUrl(req.getBaseUrl())
                    .apiKey(req.getApiKey())
                    .model(req.getModel())
                    .build());
            log.info("模型配置已持久化 userId:{} agentId:{}", req.getUserId(), req.getAgentId());
            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("更新模型配置失败", e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("更新失败：" + e.getMessage())
                    .build();
        }
    }

    @GetMapping("model_config")
    public Response<ModelConfigVO> getModelConfig(
            @RequestParam String userId,
            @RequestParam String agentId) {
        try {
            AgentModelConfigPO po = agentModelConfigDao.findByUserIdAndAgentId(userId, agentId);
            if (po == null) {
                return Response.<ModelConfigVO>builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .info(ResponseCode.SUCCESS.getInfo())
                        .data(null)
                        .build();
            }
            ModelConfigVO vo = new ModelConfigVO();
            vo.setBaseUrl(po.getBaseUrl());
            vo.setApiKey(po.getApiKey());
            vo.setModel(po.getModel());
            return Response.<ModelConfigVO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(vo)
                    .build();
        } catch (Exception e) {
            log.error("查询模型配置失败 userId:{} agentId:{}", userId, agentId, e);
            return Response.<ModelConfigVO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败：" + e.getMessage())
                    .build();
        }
    }

    @Data
    public static class ModelConfigVO {
        private String baseUrl;
        private String apiKey;
        private String model;
    }
}
