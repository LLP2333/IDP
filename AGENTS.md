# AGENTS.md

本文件是 Codex 在本仓库工作的行为规范，内容整理自 `.cursor/rules/*.mdc`。修改代码时优先遵守本文件；如规则需要更新，应同步回 `.cursor/rules` 或从 `.cursor/rules` 重新整理。

## 通用约定

- 默认使用中文与用户沟通，代码注释、文档、提交信息也优先使用中文。
- 这是一个通用企业后台系统，包含 `backend` 和 `frontend` 两个主要工程。
- 前端使用 pnpm 10；后端必须使用 Maven Wrapper，即 `./mvnw`。
- 本地依赖默认通过 Docker Compose 提供 PostgreSQL `5432` 和 Redis `6379`。
- 前端通过 `NEXT_PUBLIC_API_BASE_URL` 调用后端 REST API。
- 提交信息使用中文动词前缀，例如：`新增`、`修复`、`重构`、`文档`、`测试`。
- 修改前先理解现有代码结构和约定，保持改动范围聚焦，不随意重构无关代码。
- 遇到既有脏工作区，禁止回滚用户或其他工具已有改动；只处理当前任务相关文件。

## 后端规范

- 技术栈：Java 21、Spring Boot 4、Spring Modulith、PostgreSQL、Redis、Maven Wrapper、JUnit 5、springdoc-openapi v3。
- 按业务模块组织包结构；跨模块只依赖其他模块根包暴露的公开类型，不直接依赖其他模块的 `internal.*`。
- 跨模块协作优先使用领域事件：`ApplicationEventPublisher` 与 `@ApplicationModuleListener`，避免直接调用其他模块内部 Service。
- 数据库表名、字段名使用 `snake_case`。
- REST Controller 必须补充 OpenAPI 注解：
  - 类级别 `@Tag`
  - 接口方法 `@Operation`
  - 参数 `@Parameter`
  - DTO 与字段 `@Schema`
  - 必填字段使用 `requiredMode = Schema.RequiredMode.REQUIRED`
  - 公开且无需 JWT 的接口使用 `@SecurityRequirements`
  - 全局 OpenAPI 配置统一放在 `OpenApiConfig`
- 后端公开类、接口、方法必须写中文 Javadoc。
- 复杂私有方法应补充中文 Javadoc。
- Service 实现类中的 `@Override` 方法必须有 Javadoc，并说明可能抛出的 `BusinessException` 场景。
- 新增或修改 Service、Controller、Repository 时，需要补充对应测试；单元测试命名为 `XxxTest`，集成测试命名为 `XxxIT`。
- 涉及模块边界时，需要关注 Spring Modulith 结构校验。
- 后端完整自检命令：`cd backend && ./mvnw verify`。

## 前端规范

- 技术栈：Next.js 15 App Router、React 19、TypeScript strict、Tailwind CSS v4、React Query、Zustand、React Hook Form、Zod、lucide-react、sonner、Vitest。
- 本项目不使用 tRPC、NextAuth、Drizzle、Pages Router；接口调用使用 REST `fetch` 封装。
- 只使用 App Router，页面放在 `src/app`。
- React 组件使用函数组件和 Hooks。
- TypeScript 禁止使用 `any`；优先使用 `unknown` 并做类型收窄。
- 导入路径优先使用 `~/xxx` 别名，避免深层相对路径。
- 环境变量集中在 `src/env.js` 定义并通过 `env` 访问；除 `lib/api/http.ts` 外，不直接使用 `process.env`。
- JWT 与用户信息只通过 `auth-store` 管理，不在其他地方重复持久化。
- API 客户端统一使用 `src/lib/api/http.ts`；业务模块 API 放在 `src/lib/api/<module>.ts`；DTO 类型放在 `src/lib/api/types.ts`。
- 所有导出的函数、Hook、组件、类型必须写中文 TSDoc/JSDoc。
- Props 字段需要写内联中文注释。
- JSDoc 内容中避免出现 `*/`，防止提前闭合注释。
- 新增或修改公共工具、组件、API 客户端时，需要补充或更新 Vitest 测试。
- 前端完整自检命令：`cd frontend && pnpm check && pnpm test`。

## 功能开发流程

- 功能变更默认需要同时考虑：实现、测试、文档、自检。
- 涉及启动、环境变量、项目结构变化时，更新根目录 `readme.md` 或对应模块 README。
- 涉及后端接口变化时，更新后端相关 API 文档或 OpenAPI 注解。
- 涉及前端功能入口、页面、环境变量、脚本时，更新前端相关说明。
- 如果无法运行必要自检命令，需要在最终回复中明确说明原因和剩余风险。

## 自检命令

- 前端：`cd frontend && pnpm check && pnpm test`
- 后端：`cd backend && ./mvnw verify`

