# AI Agent Contact Server

[中文文档](./README.zh-CN.md)

`ai-agent-contact` is the backend service of an AI customer service product.

It provides agent orchestration, conversation management, model configuration, knowledge base integration, authentication, and streaming chat APIs for multiple frontend clients.

## What It Does

- Manage users, login state, and authenticated access
- Provide agent selection and agent-facing business APIs
- Create and maintain multi-session chat conversations
- Stream AI responses to frontend clients
- Manage model configuration per agent
- Integrate knowledge base and RAG-style retrieval workflows
- Serve as the shared backend for both app-style and desktop-style clients

## Product Positioning

This project is not only a chat API.

It acts as the backend control layer of an **AI customer service platform**, combining:

- agent runtime
- conversation lifecycle
- model configuration
- knowledge base access
- unified service interfaces for multiple frontend products

## Core Capabilities

### Authentication and User Access

- Login and identity verification
- Access control for frontend clients
- Stable auth flow for productized use

### Agent and Session Management

- Query available agents
- Switch among multiple agent roles
- Create new sessions
- Restore and continue historical sessions

### AI Conversation Workflow

- Multi-turn chat context management
- User / assistant message persistence
- Streaming response output for real-time UI updates

### Model Configuration

- Configure model settings by agent
- Support `baseUrl`, `apiKey`, and `model`
- Allow switching between model providers and model versions

### Knowledge Base / RAG

- Manage knowledge documents
- Attach reference material to agent workflows
- Support enterprise Q&A and internal knowledge retrieval scenarios

## Typical Use Cases

- Build the backend of an enterprise AI customer service product
- Power multiple frontend channels with one agent service layer
- Validate a product concept that combines agents, models, and knowledge base workflows
- Support AI assistants for customer service, presales, internal support, or business consulting

## Who It Is For

- Backend engineers building AI customer service or AI Q&A systems
- AI application developers who want one backend for agents, models, and knowledge
- Product managers validating whether the backend side of the product is complete enough for rollout
- Internal innovation teams piloting AI support tools with lower implementation cost

## Why It Is Better Than a Simple Chat Demo

Simple demos usually stop at “send a prompt and get an answer.” This project is closer to a real backend product:

- Multiple agents instead of a single bot
- Managed sessions instead of one-off requests
- Configurable models instead of a hard-coded provider
- Knowledge base integration instead of pure model-only replies
- Reusable service layer for multiple clients instead of a single page demo

## Project Structure

```text
ai-agent-contact-api             API layer
ai-agent-contact-app             application bootstrap
ai-agent-contact-domain          core domain logic
ai-agent-contact-infrastructure  persistence and integrations
ai-agent-contact-trigger         controllers and triggers
ai-agent-contact-types           shared types
```

## Tech Stack

- Java
- Spring Boot
- Maven
- Database and knowledge-base related integrations

## Quick Start

### Requirements

- JDK
- Maven
- a configured database and runtime environment

### Build

```bash
mvn clean install
```

### Run

Start the Spring Boot application module with the environment configuration required by the project.

Before startup, review:

- `.env.example`
- `docker-compose.yml`
- `build.md`

## Recommended Pairing

Use this backend together with:

- [`ai-agent-contact-app`](https://github.com/BlueBloodFire/agent-contact-app)
- [`ai-agent-contact-client`](https://github.com/BlueBloodFire/agent-contact-client)

Together they form the complete AI Agent Contact product:

- `ai-agent-contact`: backend services, agents, sessions, models, knowledge
- `ai-agent-contact-app`: mobile-style frontend experience
- `ai-agent-contact-client`: desktop-style web workspace
