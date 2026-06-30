package cn.wjagent.ai.config;

import cn.wjagent.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import cn.wjagent.ai.domain.agent.service.IArmoryService;
import cn.wjagent.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.wjagent.ai.infrastructure.dao.AgentModelConfigDao;
import cn.wjagent.ai.infrastructure.po.AgentModelConfigPO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiAgentAutoConfigProperties.class)
public class AiAgentAutoConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @Resource
    private IArmoryService armoryService;

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Resource
    private AgentModelConfigDao agentModelConfigDao;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            log.info("Ai Agent 正在装配中 {}", JSON.toJSONString(aiAgentAutoConfigProperties.getTables().values()));

            // 装配操作
            armoryService.acceptArmoryAgents(new ArrayList<>(aiAgentAutoConfigProperties.getTables().values()));

            // 从数据库恢复已保存的模型配置（覆盖 YAML 默认值）
            try {
                List<AgentModelConfigPO> savedConfigs = agentModelConfigDao.findAll();
                for (AgentModelConfigPO config : savedConfigs) {
                    try {
                        defaultArmoryFactory.updateModelConfig(
                                config.getAgentId(), config.getBaseUrl(), config.getApiKey(), config.getModel());
                        log.info("已从数据库恢复模型配置 agentId:{}", config.getAgentId());
                    } catch (Exception e) {
                        log.warn("恢复模型配置失败 agentId:{} {}", config.getAgentId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("加载数据库模型配置失败（忽略）: {}", e.getMessage());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
