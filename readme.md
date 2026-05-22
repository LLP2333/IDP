# IDP 项目

Interactive Design Platform，简称 **IDP**，是一个基于参数化设计模型的平台，支持 AI 驱动的参数计算。

---

## 项目结构

```text
idp/
├── backend/        # Spring Boot 后端（Spring Modulith 模块化结构）
├── frontend/       # T3 Stack 前端（Next.js + TypeScript + tRPC + Drizzle）
├── docker/         # Docker 相关脚本与配置
├── docs/           # 项目设计文档与说明
├── compose.yaml    # 本地依赖编排（PostgreSQL + Redis）
└── readme.md
```

---

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java + Spring Boot + Spring Modulith |
| 前端 | TypeScript + Next.js 15 (App Router) + React 19 + tRPC + Drizzle ORM + Tailwind CSS v4 + NextAuth |
| 数据库 | PostgreSQL |
| 缓存 | Redis |
| 单元测试 | JUnit（后端） / Vitest + Testing Library（前端） |

---

## 快速开始

### 1. 启动基础依赖

```bash
docker compose up -d
```

会拉起 PostgreSQL（5432）与 Redis（6379）。

### 2. 启动后端

详见 [`backend/readme.md`](backend/readme.md)。

```bash
cd backend
./mvnw spring-boot:run
```

### 3. 启动前端

详见 [`frontend/README.md`](frontend/README.md)。

```bash
cd frontend
pnpm install
cp .env.example .env   # 首次需要，按需填充密钥
pnpm db:push           # 推送 Drizzle schema 到 Postgres
pnpm dev
```

---

## 开发规范（关键约定）

1. **新增功能必须附带测试用例**：后端 JUnit，前端 Vitest，且 PR 中需保证测试通过。
2. **新增功能必须同步更新文档**：包括 `readme.md`、模块级 README 以及 `docs/` 下相应说明。
3. **目录规范**：
   - 后端遵循 **Spring Modulith** 包结构（参见 `backend/readme.md`）。
   - 前端遵循 **T3 Stack** 约定（参见 `frontend/README.md`）。
4. **提交前自检**：
   - 后端：`./mvnw verify`
   - 前端：`pnpm check && pnpm test`

更细致的 AI/IDE 编码规则见 `.cursor/rules/`。
