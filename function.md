# function.md — 已有功能 & 待实现路线图

## 已有功能

### 基础工程（DDD 脚手架自带）

- DDD 六模块工程骨架（api/app/domain/infrastructure/trigger/types）
- 线程池配置（ThreadPoolConfig）、Guava 配置
- Docker 部署脚本（docs/dev-ops/）
- MyBatis + MySQL 依赖（未启用）

### 客服 Agent 配置（2026-06-22）

| Agent | Agent ID | 说明 |
|-------|----------|------|
| contactWebAgent | 300001 | Web智能客服：问答咨询、PPT/Word制作、Web原型生成（Stitch MCP） |
| contactAppAgent | 300002 | App智能客服：问答咨询、情绪识别、App原型生成（Stitch MCP） |

### AI 框架集成（2026-06-12）

| 测试类 | 验证内容 |
|--------|---------|
| `springai/SpringAiModelTest` | Spring AI 直连 OpenAI 兼容接口对话 |
| `springai/SpringAiToolTest` | `@Tool` 函数调用（时间查询 + 加法） |
| `springai/SpringAiAgentTest` | ChatClient + 系统指令 + 工具 + 会话记忆的运维 Agent，两轮对话 |
| `langchain4j/LangChain4jModelTest` | LangChain4j 直连对话 |
| `langchain4j/LangChain4jToolTest` | AiServices + `@Tool` 函数调用 |
| `langchain4j/LangChain4jAgentTest` | AiServices + `@SystemMessage` + 记忆的运维 Agent，两轮对话 |
| `adk/AdkModelTest` | ADK 经 SpringAI 桥接的最小 Agent 对话 |
| `adk/AdkToolTest` | `FunctionTool` + `@Schema` 工具调用 |
| `adk/AdkAgentTest` | 完整 ReAct Agent（查状态→重启→汇报），同 Session 两轮验证记忆 |

## 待实现功能路线图

> 按优先级排列，调用时直接引用对应条目编号。

| # | 功能 | 说明 | 难度 | 状态 |
|---|------|------|------|------|
| A1 | Agent 配置化装配 | YAML 定义 agent（模型/指令/工具），启动时责任链装配 | 中 | ✅ 已完成 |
| A2 | 统一 Agent 抽象层 | 屏蔽三框架差异的统一 Agent 接口（chat/stream/tools） | 高 | 待实现 |
| A3 | SSE 流式对话接口 | trigger 层暴露 /chat_stream，推送工具调用/文本事件 | 中 | ✅ 已完成（AgentServiceController 实现 4 个接口 + 测试类） |
| A4 | MCP 工具接入 | SSE/Stdio/Local 三种 MCP 类型，枚举工厂路由，Local 支持 @Tool Bean | 中 | ✅ 已完成 |
| A4+ | 插件体系 | BasePlugin 扩展：MyLogPlugin(LoggingPlugin)、MyTestPlugin(回调日志)、PrometheusPlugin(Micrometer 监控) | 中 | ✅ 已完成 |
| A5 | 多 Agent 编排 | ADK Sequential/Parallel/Loop 三种 workflow 节点，index 递进流转 | 高 | ✅ 已完成 |
| A6 | 会话持久化 | chat_session/chat_message 表 + MyBatis（脚手架已含依赖） | 中 | 待实现 |
| A7 | RAG 知识库 | Spring AI EmbeddingModel + VectorStore | 中 | 待实现 |
| B1 | 会话持久化 | chat_session/chat_message 表，记录客服对话全程 | 中 | 待实现 |
| B2 | 情绪分析增强 | 情绪标签 + 自动转人工触发器 | 中 | 待实现 |
| B3 | 消息通知 | 订单/到期/活动主动推送（WebSocket/SSE） | 中 | 待实现 |
| B4 | 工单系统 | 复杂问题生成工单，流转给人工 | 中 | 待实现 |
