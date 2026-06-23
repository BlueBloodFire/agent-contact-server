# agent-scaffold

AI Agent 开发脚手架，基于 Spring Boot 3 + DDD 架构，集成三大 AI Agent 框架：

| 框架 | 用途 |
|------|------|
| Spring AI | ChatClient + @Tool 函数调用 + Advisor 会话记忆 |
| LangChain4j | AiServices 动态代理 + @Tool + ChatMemory |
| Google ADK | LlmAgent + FunctionTool + InMemoryRunner（Spring AI 桥接 OpenAI 兼容接口） |

## 快速开始

```bash
# 编译
mvn clean package -DskipTests

# 运行 AI 测试（先在 TestApiConfig 中配置你的 apikey）
# IDE 中直接运行 agent-scaffold-app/src/test/java/cn/wjagent/ai/test/ 下各测试类的 main 方法
```

## 文档

- [build.md](build.md) — 项目概述、构建 & 运行
- [framework.md](framework.md) — 目录结构、架构模式
- [function.md](function.md) — 已有功能、待实现路线图
- [change.md](change.md) — 进度 & 修改记录
