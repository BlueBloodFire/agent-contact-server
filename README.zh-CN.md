# AI Agent Contact Server

[English](./README.md)

`ai-agent-contact` 是 AI 客服产品的后端服务。

它负责智能体编排、会话管理、模型配置、知识库接入、用户认证以及流式对话接口，为多个前端产品提供统一的服务能力。

## 项目作用

- 管理用户、登录状态和鉴权访问
- 提供智能体选择与业务接口
- 创建并维护多会话聊天记录
- 向前端输出流式 AI 回复
- 按智能体维护模型配置
- 集成知识库与 RAG 检索流程
- 作为 App 端和 Web 工作台的统一后端

## 产品定位

这个项目不是单纯的聊天接口。

它更像是一个 **AI 客服平台后端中台**，把以下能力统一收口到服务端：

- 智能体运行时
- 会话生命周期
- 模型配置
- 知识库访问
- 多前端共享接口

## 核心能力

### 用户认证与访问控制

- 登录与身份校验
- 面向前端产品的访问控制
- 支撑产品化使用的统一鉴权流程

### 智能体与会话管理

- 查询可用智能体
- 在多个智能体角色之间切换
- 创建新会话
- 恢复并延续历史会话

### AI 对话流程

- 管理多轮上下文
- 持久化用户消息与 AI 回复
- 提供流式输出，便于前端实时展示

### 模型配置

- 按智能体配置模型参数
- 支持 `baseUrl`、`apiKey`、`model`
- 便于切换不同模型服务商和版本

### 知识库 / RAG

- 管理知识库文档
- 将资料接入智能体问答流程
- 支撑企业客服、内部问答、业务咨询等场景

## 典型使用场景

- 搭建企业 AI 客服产品的后端
- 为多个前端渠道复用同一套智能体服务
- 验证“多智能体 + 多模型 + 知识库”产品方案
- 支撑客服、售前、内部支持或业务顾问类 AI 助手

## 适合谁使用

- 后端工程师：需要快速搭建 AI 客服或 AI 问答系统服务层
- AI 应用开发者：希望统一管理智能体、模型和知识库能力
- 产品经理：验证后端能力是否足够支撑产品落地
- 企业创新团队：低成本试点 AI 支持类产品

## 相比简单聊天 Demo 的优势

普通 Demo 往往只做到“输入问题，返回答案”；本项目更接近真实后端产品：

- 支持多智能体，而不是单一 Bot
- 支持完整会话管理，而不是一次性问答
- 支持模型配置切换，而不是写死单一模型
- 支持知识库接入，而不是纯模型裸聊
- 支持多前端复用，而不是只服务一个页面

## 项目结构

```text
ai-agent-contact-api             接口层
ai-agent-contact-app             应用启动模块
ai-agent-contact-domain          核心领域逻辑
ai-agent-contact-infrastructure  持久化与基础设施集成
ai-agent-contact-trigger         控制器与触发器
ai-agent-contact-types           公共类型
```

## 技术栈

- Java
- Spring Boot
- Maven
- 数据库及知识库相关集成能力

## 快速启动

### 环境要求

- JDK
- Maven
- 已配置的数据库和运行环境

### 构建

```bash
mvn clean install
```

### 运行

按项目要求配置环境后，启动对应的 Spring Boot 应用模块。

启动前建议先查看：

- `.env.example`
- `docker-compose.yml`
- `build.md`

## 搭配使用

建议配合以下前端项目一起使用：

- [`ai-agent-contact-app`](https://github.com/BlueBloodFire/agent-contact-app)
- [`ai-agent-contact-client`](https://github.com/BlueBloodFire/agent-contact-client)

三者共同组成完整产品：

- `ai-agent-contact`：后端服务、智能体、会话、模型、知识库
- `ai-agent-contact-app`：偏移动端 / App 风格前端
- `ai-agent-contact-client`：偏桌面工作台 / Web 后台前端

## License

本项目使用 Apache License 2.0 授权，请查看 [LICENSE](./LICENSE) 文件。
