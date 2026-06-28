package cn.wjagent.ai.config;

import cn.wjagent.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.wjagent.ai.infrastructure.dao.AgentModelConfigDao;
import cn.wjagent.ai.infrastructure.po.AgentModelConfigPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ModelConfigInitializer implements ApplicationRunner {

    @Resource
    private AgentModelConfigDao agentModelConfigDao;

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<AgentModelConfigPO> configs = agentModelConfigDao.findAll();
            if (configs.isEmpty()) {
                log.info("数据库无模型配置记录，使用 yml 默认配置");
                return;
            }
            for (AgentModelConfigPO cfg : configs) {
                try {
                    // 按用户恢复各自的 ChatModel 到内存缓存
                    defaultArmoryFactory.updateUserModelConfig(
                            cfg.getUserId(), cfg.getAgentId(), cfg.getBaseUrl(), cfg.getApiKey(), cfg.getModel());
                    log.info("从数据库恢复用户模型配置 userId:{} agentId:{} model:{}",
                            cfg.getUserId(), cfg.getAgentId(), cfg.getModel());
                } catch (Exception e) {
                    log.warn("恢复模型配置失败 userId:{} agentId:{} 原因:{}",
                            cfg.getUserId(), cfg.getAgentId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("加载数据库模型配置异常，跳过恢复: {}", e.getMessage());
        }
    }
}
