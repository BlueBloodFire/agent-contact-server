# change.md — 进度 & 修改记录（最新在上）

## 2026-06-28 · Plan B — Direct 模式（Spring AI 直接流式）

**背景**：ADK `InMemoryRunner.runAsync()` 不做 token 级流式，整个 agent 完成后才发 Event，导致首字延迟高。Plan B 新增 `direct` 模式，绕过 ADK 直接用 Spring AI `ChatModel.stream()` 输出 token。

**变更文件**

| 文件 | 说明 |
|------|------|
| `domain/valobj/AiAgentConfigTableVO.Module` | 新增 `mode`（字符串）和 `systemPrompt` 字段 |
| `domain/valobj/AiAgentRegisterVO` | 新增 `chatModel`、`systemPrompt`、`directMode` 字段 |
| `domain/node/DirectRunnerNode` | 新增叶子节点：不创建 InMemoryRunner，直接返回带 ChatModel 的 VO |
| `domain/node/ChatModelNode` | `get()` 方法判断 `mode == "direct"` 时路由到 `DirectRunnerNode` |
| `domain/service/IChatService` | 新增 `isDirectMode(agentId)` 和 `handleMessageStreamDirect(...)` 两个方法 |
| `domain/service/ChatService` | 实现 direct 流式：Spring AI stream → Flowable\<String\>，维护多轮历史（最多20轮） |
| `trigger/AgentServiceController` | `chat_stream` 判断 direct 模式，分支调用对应流式方法 |
| `resources/agent/direct-chat-agent.yml` | 新增 agentId=300003，通用智能助手，mode=direct，model=deepseek-v3 |
| `resources/application-dev.yml` | spring.config.import 增加 `classpath:agent/direct-chat-agent.yml` |

**效果**：direct 模式 TTFT 从数秒降至 <1s（token 级实时流式输出）

---

## 2026-06-28 · 前端样式深度优化

**参考 Claude Desktop UX 模式，提升对话体验：**

### 代码块增强
- 新增 `rehype-highlight` + `highlight.js` 依赖，代码块显示语法高亮（github-dark 主题）
- 代码块右上角添加"复制"按钮，复制后变绿色"已复制"1.5s
- 有语言标注的代码块显示语言名称标题栏（暗色）
- 行内代码保持紫色风格

### 流式体验优化
- 流式输出时在消息末尾显示闪烁光标 `|`（替代之前独占一行的跳动圆点）
- 三点跳动仅在消息内容完全为空时显示（等待首个 chunk）

### 停止响应
- `contactStore` 新增 `stopStream()` + `_abortFn`，支持取消正在进行的流式请求
- 流式响应期间，发送按钮变为红色"停止"按钮（方形图标），点击立即中断

### ChatWindow 顶部标题栏
- 对话区顶部增加固定标题栏，显示当前 Agent 名称 + 描述
- 无会话时的欢迎页重新设计，包含 Agent 图标、名称、描述，按钮更大更醒目

**变更文件**：
- `client/src/components/ChatMessage.tsx` — CodeBlock 组件、rehype-highlight、流式光标
- `client/src/components/ChatWindow.tsx` — 顶部标题栏、重设计欢迎页
- `client/src/components/ChatInput.tsx` — 停止按钮逻辑
- `client/src/stores/contactStore.ts` — stopStream() / _abortFn
- `client/src/index.css` — 引入 highlight.js CSS + @tailwindcss/typography

---

## 2026-06-28 · 流式响应进阶优化（参考 Claude 源码）

**参考 Claude 源码中的关键模式**：
- TTFT（Time-to-First-Token）埋点：`queryCheckpoint('query_first_chunk_received')`，记录从请求开始到首个有效 chunk 的耗时
- 流式看门狗（Stream Idle Watchdog）：90秒无数据强制超时并通知客户端
- 卡顿检测（Stall Detection）：30秒无事件时打印警告日志 `tengu_streaming_stall`

**本项目变更**：
- `trigger/AgentServiceController`：`chat_stream` / `chat_multimodal_stream`
  - 新增 `ScheduledExecutorService watchdog`（4线程，守护线程）
  - 每10秒检查距上次事件间隔：≥30s 打警告日志，≥90s 发送 `[TIMEOUT]` 给前端并强制 complete
  - 新增 TTFT 日志：首个非空 chunk 触发时记录 `流式首次响应(TTFT) 耗时Xms`
  - 新增总耗时日志：stream complete 时记录 `流式对话完成 总耗时Xms`
- `client/src/api/request.ts`：`streamPost` / `streamMultipart`
  - 检测到 `[TIMEOUT]` 时调用 `onError('响应超时，请稍后重试')` 并取消 reader

---

## 2026-06-28 · 响应慢问题排查与优化

**根因**：ADK `runAsync()` 不做 token 级流式（整个 agent 完成后才发 Event）；bingSearchTool 每次对话可能触发额外 HTTP 请求；ResponseBodyEmitter 未设 Content-Type 导致浏览器缓冲。

- `trigger/AgentServiceController`：`chat_stream` / `chat_multimodal_stream` 加 `produces = text/event-stream`，过滤空 content，每个 chunk 指定 `MediaType.TEXT_PLAIN`
- `domain/ChatService`：新增 `sessionCache` Map 缓存 Session 对象，`resolveSession` 先查缓存
- `agent/contact-web-agent.yml` + `contact-app-agent.yml`：search tool 改为"仅用户明确要求时才调用"，避免每次对话都触发 Tavily 网络请求

---

## 2026-06-28 · 认证 + 动态模型配置 + RAG

### 用户认证
- 新增 `LoginRequestDTO.java`、`LoginResponseDTO.java`（api 层）
- 新增 `AuthController.java`（trigger 层）：`POST /api/v1/login`、`POST /api/v1/logout`；硬编码3个账号（admin/rootUser/testUser，密码同账号），Token 内存存储 8h TTL
- 新增 `AuthInterceptor.java`（app/config）：拦截所有 `/api/v1/**` 接口，验证 Bearer token，过期返回 401
- 新增 `WebMvcConfig.java`（app/config）：注册拦截器，排除 `/api/v1/login`

### 动态模型配置
- 新增 `UpdateModelConfigRequestDTO.java`（api 层）
- `DefaultArmoryFactory.java`：新增 `updateModelConfig()` 方法，支持运行时修改 agentId 对应的 baseUrl/apiKey/model 并重新装配 runner
- 新增 `ModelConfigController.java`（trigger 层）：`POST /api/v1/update_model_config`

### RAG 功能（关键词匹配，无需 embedding 服务）
- 新增 `IRagService.java`（domain 层接口）
- 新增 `RagService.java`（domain/chat）：文档 500字符分块存储，关键词打分检索，topK 返回
- 新增 `RagController.java`（trigger 层）：`POST /api/v1/rag/upload`、`GET /api/v1/rag/documents`、`GET /api/v1/rag/search`
- `ChatService.java`：`handleMessageStream` 新增 RAG 上下文注入（检索结果拼在用户消息前）

## 2026-06-24 · 搜索引擎切换 Bing → Tavily

- `BingSearchTool.java`：请求改为 POST `api.tavily.com/search`，解析 `results[].title/url/content`；移除 URLEncoder 依赖
- `application-dev.yml`：配置键 `bing.search.api-key` 改为 `tavily.search.api-key`，对应环境变量 `TAVILY_API_KEY`

## 2026-06-22

### 客服智能体配置 + MCP Header 支持

**变更内容**
- `AiAgentConfigTableVO.SSEServerParameters` 新增 `headers: Map<String, String>` 字段，支持自定义 HTTP 请求头
- `SSEToolMcpCreateService` 更新：读取 `headers` 配置，通过 `customizeRequest` 注入到 `HttpClientSseClientTransport`
- 新增 `contact-web-agent.yml`：Web 端智能客服（agentId=300001），集成 Google Stitch MCP（Web 原型生成）
- 新增 `contact-app-agent.yml`：App 端智能客服（agentId=300002），集成 Google Stitch MCP（App 原型生成）
- 更新 `function.md`：补充客服功能条目（B1-B4）

**相关文件**
- `domain/agent/model/valobj/AiAgentConfigTableVO.java`
- `domain/agent/service/armory/factory/matter/mcp/client/impl/SSEToolMcpCreateService.java`
- `app/src/main/resources/agent/contact-web-agent.yml`（含密钥，不提交 GitHub）
- `app/src/main/resources/agent/contact-app-agent.yml`（含密钥，不提交 GitHub）

**注意**
- `customizeRequest` 依赖 io.modelcontextprotocol:java-sdk ≥ 0.8.0，如编译失败需降级为手动 HttpClient 实现
- Google Stitch MCP 使用 HTTP transport（非传统 SSE），连接可能需要调整 `sse-endpoint` 配置
