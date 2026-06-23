package cn.wjagent.ai.config;

import cn.wjagent.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import cn.wjagent.ai.domain.agent.service.IArmoryService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.ArrayList;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiAgentAutoConfigProperties.class)
public class AiAgentAutoConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @Resource
    private IArmoryService armoryService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            log.info("Ai Agent 正在装配中 {}", JSON.toJSONString(aiAgentAutoConfigProperties.getTables().values()));

            // 装配操作
            armoryService.acceptArmoryAgents(new ArrayList<>(aiAgentAutoConfigProperties.getTables().values()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
