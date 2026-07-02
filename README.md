# AI Agent Contact Server

`ai-agent-contact` 是 AI 客服产品的后端服务，负责承载智能体运行、会话编排、流式对话、模型配置、知识库接入以及服务端状态管理。

它不是一个单纯的“聊天接口工程”，而是整个 AI 客服产品的 **Agent Runtime + API Backend**。

## 产品定位

这个项目面向“AI 客服平台”场景，目标是把客服问答、业务咨询、知识检索、模型切换、会话管理等能力统一沉淀到服务端。

前端和 App 只负责交互体验，真正的智能体执行、上下文管理和接口输出都由本仓库负责。

## 核心功能

### 1. 智能体管理

- 提供多个客服智能体配置
- 支持按 `agentId` 查询可用智能体列表
- 区分不同客服角色，例如 Web 客服、App 客服、通用对话助手

### 2. 会话与对话能力

- 创建独立会话 `create_session`
- 普通问答接口 `chat`
- 流式输出接口 `chat_stream`
- 支持多轮上下文延续
- 支持会话标题、轮次、消息记录持久化

### 3. AI Agent 编排能力

- 集成多套 AI Agent 技术路线：
  - Spring AI
  - LangChain4j
  - Google ADK
- 支持工具调用、流式回复、上下文记忆
- 适合作为后续客服流程自动化和能力实验平台

### 4. 模型与配置管理

- 支持动态切换模型配置
- 支持不同智能体对应不同模型参数
- 可用于多模型试验、多供应商兼容和运行时热更新

### 5. 知识库 / RAG 能力

- 提供知识库接入能力
- 支持把业务资料、FAQ、文档内容注入客服上下文
- 为后续精准问答、资料检索和客服增强打基础

### 6. 认证与服务端控制

- 支持用户认证
- 统一封装 API 输出格式
- 适合作为 Web 端和 App 端的统一后端

## 典型使用场景

- 做一个企业内部 AI 客服平台，统一接入多个客服智能体
- 为 App 客服和 Web 客服提供同一套后端能力
- 做 FAQ、资料问答、业务咨询、表单引导等 AI 客服功能
- 给不同业务线配置不同 Agent、不同模型和不同知识库
- 把“模型试验场”逐步演进成“可上线客服后端”

## 仓库结构

```text
ai-agent-contact-api             对外接口定义、DTO、响应结构
ai-agent-contact-app             启动模块、配置、资源、测试
ai-agent-contact-domain          领域服务、Agent 业务逻辑
ai-agent-contact-infrastructure  持久化、DAO、配置、外部适配
ai-agent-contact-trigger         HTTP 控制器、接口入口
ai-agent-contact-types           通用枚举、异常、常量
```

## 对外接口能力

主要接口方向包括：

- 查询智能体配置列表
- 创建对话会话
- 非流式对话
- 流式对话
- 历史消息恢复
- 模型配置更新
- 知识库相关接口

## 技术栈

- Java 17
- Spring Boot 3
- MyBatis
- MySQL
- Spring AI
- LangChain4j
- Google ADK
- SSE 流式输出

## 快速启动

### 环境要求

- JDK 17
- Maven 3.8+
- MySQL

### 启动

```bash
mvn clean package -DskipTests
java -jar ai-agent-contact-app/target/agent-scaffold-app.jar
```

也可以结合仓库中的：

- `docker-compose.yml`
- `Dockerfile`
- `deploy.sh`
- `deploy.py`

进行容器化和远程部署。

## 相关前端仓库

这个后端通常和以下两个前端项目配套使用：

- `ai-agent-contact-app`：偏 App 风格的客服前端
- `ai-agent-contact-client`：偏 Web 工作台风格的客服平台前端

三者关系如下：

- `ai-agent-contact`：后端、智能体运行时、接口服务
- `ai-agent-contact-app`：移动端 / App 风格体验层
- `ai-agent-contact-client`：桌面 Web / 管理工作台体验层

## 适合访客快速理解的一句话

这是一个面向 AI 客服产品的后端平台，负责把“智能体、会话、模型、知识库、流式输出”统一变成可供 Web 和 App 使用的服务。
