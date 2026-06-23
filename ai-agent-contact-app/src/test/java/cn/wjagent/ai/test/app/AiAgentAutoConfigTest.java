package cn.wjagent.ai.test.app;

import cn.wjagent.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AiAgentAutoConfigTest {

    @Resource
    protected ApplicationContext applicationContext;

    @Test
    public void test_agent_register() {
        AiAgentRegisterVO vo = applicationContext.getBean("100001", AiAgentRegisterVO.class);

        log.info("装配结果 appName={} agentId={} agentName={}", vo.getAppName(), vo.getAgentId(), vo.getAgentName());

        Assert.assertNotNull("Runner 不应为空", vo.getRunner());
        Assert.assertEquals("testAgent01", vo.getAppName());
        Assert.assertEquals("100001", vo.getAgentId());
    }

    @Test
    public void test_agent_pipeline() {
        AiAgentRegisterVO vo = applicationContext.getBean("100001", AiAgentRegisterVO.class);

        InMemoryRunner runner = vo.getRunner();
        Session session = runner.sessionService()
                .createSession(vo.getAppName(), "test-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("编写一个二分查找算法"));
        Flowable<Event> events = runner.runAsync("test-user", session.id(), userMsg);

        List<String> finalOutputs = events.filter(Event::finalResponse)
                .map(Event::stringifyContent)
                .toList()
                .blockingGet();

        Assert.assertFalse("Pipeline 应有最终输出", finalOutputs.isEmpty());
        log.info("Pipeline 最终输出:\n{}", String.join("\n---\n", finalOutputs));
    }

    @Test
    public void test_plugin_log() {
        // 验证 myLogPlugin / myTestPlugin 正常挂载并拦截调用（观察日志输出）
        AiAgentRegisterVO vo = applicationContext.getBean("100001", AiAgentRegisterVO.class);

        InMemoryRunner runner = vo.getRunner();
        Session session = runner.sessionService()
                .createSession(vo.getAppName(), "plugin-log-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("你好，给我推荐一本书"));
        List<String> outputs = runner.runAsync("plugin-log-user", session.id(), userMsg)
                .filter(Event::finalResponse)
                .map(Event::stringifyContent)
                .toList()
                .blockingGet();

        Assert.assertFalse("插件链路应有最终输出", outputs.isEmpty());
        log.info("Plugin Log 输出:\n{}", String.join("\n---\n", outputs));
    }

    @Test
    public void test_plugin_prometheus_metrics() {
        // 验证 PrometheusPlugin 正确记录了 Agent 调用次数和耗时
        AiAgentRegisterVO vo = applicationContext.getBean("100001", AiAgentRegisterVO.class);
        MeterRegistry meterRegistry = applicationContext.getBean(MeterRegistry.class);

        InMemoryRunner runner = vo.getRunner();
        Session session = runner.sessionService()
                .createSession(vo.getAppName(), "prometheus-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("用一句话介绍 Java"));
        runner.runAsync("prometheus-user", session.id(), userMsg)
                .filter(Event::finalResponse)
                .toList()
                .blockingGet();

        // 断言 agent 调用计数
        Counter agentCounter = meterRegistry.find("agent.invocation.count").counter();
        Assert.assertNotNull("agent.invocation.count 指标应存在", agentCounter);
        Assert.assertTrue("agent 调用次数应 > 0", agentCounter.count() > 0);
        log.info("agent.invocation.count = {}", agentCounter.count());

        // 断言 agent 耗时
        Timer agentTimer = meterRegistry.find("agent.invocation.duration").timer();
        Assert.assertNotNull("agent.invocation.duration 指标应存在", agentTimer);
        log.info("agent.invocation.duration totalTime = {}ms",
                agentTimer.totalTime(TimeUnit.MILLISECONDS));

        // 断言 model 调用计数
        Counter modelCounter = meterRegistry.find("model.call.count").counter();
        Assert.assertNotNull("model.call.count 指标应存在", modelCounter);
        Assert.assertTrue("model 调用次数应 > 0", modelCounter.count() > 0);
        log.info("model.call.count = {}", modelCounter.count());

        // 断言 model 耗时
        Timer modelTimer = meterRegistry.find("model.call.duration").timer();
        Assert.assertNotNull("model.call.duration 指标应存在", modelTimer);
        log.info("model.call.duration totalTime = {}ms",
                modelTimer.totalTime(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_mcp_tool_time() {
        AiAgentRegisterVO vo = applicationContext.getBean("200001", AiAgentRegisterVO.class);

        log.info("MCP 装配结果 appName={} agentId={} agentName={}", vo.getAppName(), vo.getAgentId(), vo.getAgentName());
        Assert.assertNotNull("MCP Runner 不应为空", vo.getRunner());

        InMemoryRunner runner = vo.getRunner();
        Session session = runner.sessionService()
                .createSession(vo.getAppName(), "mcp-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("现在几点了？请用 getCurrentTime 工具查询"));
        Flowable<Event> events = runner.runAsync("mcp-user", session.id(), userMsg);

        List<String> finalOutputs = events.filter(Event::finalResponse)
                .map(Event::stringifyContent)
                .toList()
                .blockingGet();

        Assert.assertFalse("MCP 工具调用应有最终输出", finalOutputs.isEmpty());
        log.info("MCP 工具调用输出:\n{}", String.join("\n---\n", finalOutputs));
    }

    @Test
    public void test_mcp_tool_add() {
        AiAgentRegisterVO vo = applicationContext.getBean("200001", AiAgentRegisterVO.class);

        InMemoryRunner runner = vo.getRunner();
        Session session = runner.sessionService()
                .createSession(vo.getAppName(), "mcp-user")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("请用 add 工具计算 18 + 24 等于多少"));
        Flowable<Event> events = runner.runAsync("mcp-user", session.id(), userMsg);

        List<String> finalOutputs = events.filter(Event::finalResponse)
                .map(Event::stringifyContent)
                .toList()
                .blockingGet();

        Assert.assertFalse("MCP add 工具应有最终输出", finalOutputs.isEmpty());
        log.info("MCP add 工具输出:\n{}", String.join("\n---\n", finalOutputs));

        String output = String.join("", finalOutputs);
        Assert.assertTrue("输出应包含计算结果 42", output.contains("42"));
    }

}
