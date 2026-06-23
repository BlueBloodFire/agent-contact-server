package cn.wjagent.ai.domain.agent.service.armory.factory.matter.plugin;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.CallbackContext;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.plugins.BasePlugin;
import com.google.genai.types.Content;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Prometheus 监控插件：追踪 Agent 调用次数 / 耗时 以及模型调用次数 / 耗时。
 *
 * 指标清单：
 *   agent.invocation.count{agent_name}  — Agent 调用计数
 *   agent.invocation.duration{agent_name} — Agent 执行耗时
 *   model.call.count{model}             — 模型调用计数
 *   model.call.duration                 — 模型调用耗时
 */
@Slf4j
@Service("prometheusPlugin")
public class PrometheusPlugin extends BasePlugin {

    private final MeterRegistry meterRegistry;

    // 用线程 ID 作 key，保证并行 Agent 各自独立计时
    private final ConcurrentHashMap<Long, Timer.Sample> agentTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Timer.Sample> modelTimers = new ConcurrentHashMap<>();

    public PrometheusPlugin(MeterRegistry meterRegistry) {
        super("PrometheusPlugin");
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Maybe<Content> beforeAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        String agentName = agent.name();
        Counter.builder("agent.invocation.count")
                .tag("agent_name", agentName)
                .description("Agent 调用次数")
                .register(meterRegistry)
                .increment();
        agentTimers.put(Thread.currentThread().getId(), Timer.start(meterRegistry));
        log.info("[Prometheus] Agent 开始执行: {}", agentName);
        return Maybe.empty();
    }

    @Override
    public Maybe<Content> afterAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        String agentName = agent.name();
        Timer.Sample sample = agentTimers.remove(Thread.currentThread().getId());
        if (sample != null) {
            sample.stop(Timer.builder("agent.invocation.duration")
                    .tag("agent_name", agentName)
                    .description("Agent 执行耗时")
                    .register(meterRegistry));
        }
        log.info("[Prometheus] Agent 执行完毕: {}", agentName);
        return Maybe.empty();
    }

    @Override
    public Maybe<LlmResponse> beforeModelCallback(CallbackContext callbackContext, LlmRequest llmRequest) {
        String modelName = llmRequest.model().orElse("unknown");
        Counter.builder("model.call.count")
                .tag("model", modelName)
                .description("模型调用次数")
                .register(meterRegistry)
                .increment();
        modelTimers.put(Thread.currentThread().getId(), Timer.start(meterRegistry));
        log.info("[Prometheus] Model 开始调用: {}", modelName);
        return Maybe.empty();
    }

    @Override
    public Maybe<LlmResponse> afterModelCallback(CallbackContext callbackContext, LlmResponse llmResponse) {
        Timer.Sample sample = modelTimers.remove(Thread.currentThread().getId());
        if (sample != null) {
            sample.stop(Timer.builder("model.call.duration")
                    .description("模型调用耗时")
                    .register(meterRegistry));
        }
        log.info("[Prometheus] Model 调用完毕");
        return Maybe.empty();
    }
}
