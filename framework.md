# framework.md — 目录结构 & 架构模式

## 模块结构

```
agent-scaffold/
├── agent-scaffold-api            ← 对外接口 DTO + 服务接口（无实现）
├── agent-scaffold-app            ← 启动类 + 配置 + YAML + AI 测试
│   └── src/test/java/cn/wjagent/ai/test/
│       ├── TestApiConfig.java    ← AI 接口连接参数（baseUrl/apiKey/model）统一配置
│       ├── springai/             ← Spring AI：Model / Tool / Agent 测试
│       ├── langchain4j/          ← LangChain4j：Model / Tool / Agent 测试
│       └── adk/                  ← Google ADK：Model / Tool / Agent 测试
├── agent-scaffold-domain         ← 领域层：服务实现 + 模型
├── agent-scaffold-infrastructure ← 基础设施：DAO + 外部适配器
├── agent-scaffold-trigger        ← 触发层：HTTP Controller
├── agent-scaffold-types          ← 通用枚举 + 异常
└── docs/dev-ops/                 ← Docker compose + SQL 脚本
```

## 架构模式

### DDD 分层（依赖方向）

`trigger → domain ← infrastructure`，app 聚合启动。
domain 不依赖 infrastructure（端口-适配器：domain 定义接口，infrastructure 实现）。

### AI 框架接入模式

三个框架统一走 **OpenAI 兼容接口**（baseUrl + apiKey + model）：

1. **Spring AI**：`OpenAiApi.builder()` → `OpenAiChatModel` → `ChatClient`（工具用 `@Tool` 注解，记忆用 `MessageChatMemoryAdvisor`）
2. **LangChain4j**：`OpenAiChatModel.builder()`（注意 baseUrl 要带 `/v1`）→ `AiServices` 动态代理接口（工具 `@Tool`、记忆 `MessageWindowChatMemory`、系统指令 `@SystemMessage`）
3. **Google ADK**：复用 Spring AI 的 ChatModel，经 `new SpringAI(chatModel)` 桥接 → `LlmAgent.builder()`（工具 `FunctionTool.create(obj, "method")` + `@Schema` 注解）→ `InMemoryRunner` 执行，Session 由 `runner.sessionService().createSession()` 创建

### 关键约定

- ADK 工具方法必须 `public`，参数用 `@Schema(name, description)` 注解，返回值推荐 `Map<String, Object>`
- LangChain4j 的 baseUrl 末尾追加 `/v1`（它不自动拼 completionsPath）
- Spring AI 的 completionsPath/embeddingsPath 显式指定 `v1/chat/completions` / `v1/embeddings`
