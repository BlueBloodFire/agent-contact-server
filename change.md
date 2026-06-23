# change.md — 进度 & 修改记录（最新在上）

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
