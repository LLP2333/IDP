# IDP 项目

IDP 是一个通用的企业级后台管理系统。

---

## 项目结构

```text
idp/
├── backend/        # Spring Boot 后端（Spring Modulith 模块化结构）
├── frontend/       # Next.js 15 前端（App Router + React 19 + Tailwind v4）
├── docker/         # Docker 相关脚本与配置
├── docs/           # 项目设计文档与说明
├── compose.yaml    # 本地依赖编排（PostgreSQL + Redis）
├── thirdPart/      # 第三方开源项目用来参考（仅参考代码逻辑）
└── readme.md
```

---

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 21 + Spring Boot 4 + Spring Modulith + Spring Security + JWT |
| 前端 | TypeScript + Next.js 15 (App Router) + React 19 + TanStack Query + Zustand + React Hook Form + Tailwind CSS v4 |
| 数据库 | PostgreSQL（用户/角色/关联） |
| 缓存 | Redis（JWT Token Store） |
| 认证 | 后端发放 JWT，前端通过 fetch 直连 REST API |
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

启动成功后会自动初始化默认角色 `admin / user` 与默认账号 **`admin / 123456`**。

### 3. 启动前端

详见 [`frontend/README.md`](frontend/README.md)。

```bash
cd frontend
pnpm install
cp .env.example .env   # 首次需要
pnpm dev
```

浏览器访问 <http://localhost:3000> 会跳转到 `/login`，使用 `admin / 123456` 登录。

后端 Swagger UI / OpenAPI 文档：

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

---

## 业务模块

| 模块 | 说明 | 文档 |
| --- | --- | --- |
| 认证 | 账号密码登录、JWT 颁发/校验、登出 | [`docs/auth.md`](docs/auth.md) |
| 用户管理 | 用户 CRUD、分配角色、重置密码 | [`docs/user-role.md`](docs/user-role.md) |
| 角色管理 | 角色 CRUD、按 code 唯一约束 | [`docs/user-role.md`](docs/user-role.md) |

---

## 开发规范（关键约定）

1. **新增功能必须附带测试用例**：后端 JUnit，前端 Vitest，且 PR 中需保证测试通过。
2. **新增功能必须同步更新文档**：包括 `readme.md`、模块级 README 以及 `docs/` 下相应说明。
3. **新增 / 修改对外 API 必须补 OpenAPI 注解**：`@Tag` / `@Operation` / `@Schema`，详见 `.cursor/rules/api-doc-comments.mdc`。
4. **所有公开函数 / 组件必须有中文 JSDoc / Javadoc**：禁止占位 / 复读式注释；JSDoc 中避免出现 `*/` 序列。
5. **目录规范**：
   - 后端遵循 **Spring Modulith** 包结构（参见 `backend/readme.md`）。
   - 前端按业务页面 + 组件 + API 客户端组织（参见 `frontend/README.md`）。
6. **提交前自检**：
   - 后端：`./mvnw verify`
   - 前端：`pnpm check && pnpm test`

更细致的 AI/IDE 编码规则见 `.cursor/rules/`。
