# build.md — 项目概述 & 构建运行

## 项目概述

agent-scaffold 是 AI Agent 开发脚手架，基于 **Spring Boot 3 + DDD** 架构，
集成三大 AI Agent 框架用于对比与开发：

| 框架 | 版本 | 用途 |
|------|------|------|
| Spring AI | BOM 1.1.5 | ChatClient + @Tool 函数调用 + Advisor 记忆 |
| LangChain4j | BOM 1.4.0 | AiServices 动态代理 + @Tool + ChatMemory |
| Google ADK | 1.2.0 | LlmAgent + FunctionTool + InMemoryRunner（经 google-adk-spring-ai 桥接 OpenAI 兼容接口） |

- JDK 17 · Maven · Spring Boot 3.4.3
- 包根：`cn.wjagent.ai`
- 启动类：`agent-scaffold-app` 模块 `cn.wjagent.ai.Application`

## 构建 & 运行

```bash
# 编译打包（跳过测试）
mvn clean package -DskipTests

# 仅编译 app 模块及其依赖（验证测试代码）
mvn test-compile -pl agent-scaffold-app -am

# 启动
java -jar agent-scaffold-app/target/agent-scaffold-app.jar

# Docker（见 docs/dev-ops/）
agent-scaffold-app/build.sh
```

## AI 测试运行

测试位于 `agent-scaffold-app/src/test/java/cn/wjagent/ai/test/`，均为带 `main` 方法的可独立运行类（IDE 直接右键 Run）。

**运行前提**：修改 `cn.wjagent.ai.test.TestApiConfig` 中的 `API_KEY` 为你自己的 apikey（OpenAI 兼容接口，默认 `https://apis.itedus.cn`，模型 `gpt-4.1`）。
