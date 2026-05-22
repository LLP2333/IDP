# IDP 前端

基于 [T3 Stack](https://create.t3.gg/) 搭建的 Next.js 前端应用。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 语言 | TypeScript 5 |
| 框架 | Next.js 15（App Router） + React 19 |
| 样式 | Tailwind CSS v4 |
| API 层 | tRPC v11 + TanStack React Query |
| 鉴权 | NextAuth v5（beta） |
| ORM | Drizzle ORM（PostgreSQL，驱动 `node-postgres`） |
| 校验 | Zod + `@t3-oss/env-nextjs` |
| 测试 | Vitest + Testing Library + jsdom |
| 代码风格 | ESLint + Prettier + `prettier-plugin-tailwindcss` |
| 包管理 | pnpm 10 |

## 目录结构

```text
frontend/
├── src/
│   ├── app/                # Next.js App Router 路由与页面
│   │   ├── _components/    # 仅本路由复用的组件
│   │   ├── api/            # Next.js Route Handlers (含 NextAuth 与 tRPC)
│   │   ├── layout.tsx
│   │   └── page.tsx
│   ├── lib/                # 通用工具方法（含单元测试 *.test.ts）
│   ├── server/             # 仅服务端代码
│   │   ├── api/            # tRPC routers
│   │   ├── auth/           # NextAuth 配置
│   │   └── db/             # Drizzle schema + 客户端
│   ├── styles/             # 全局样式
│   ├── trpc/               # tRPC 客户端封装
│   └── env.js              # 服务端/客户端环境变量校验
├── drizzle.config.ts
├── eslint.config.js
├── next.config.js
├── postcss.config.js
├── prettier.config.js
├── tsconfig.json
├── vitest.config.ts
├── vitest.setup.ts
└── package.json
```

## 环境准备

```bash
pnpm install
cp .env.example .env
```

需要的环境变量：

| 变量 | 说明 |
| --- | --- |
| `AUTH_SECRET` | NextAuth 加密密钥，可用 `npx auth secret` 生成 |
| `AUTH_DISCORD_ID` / `AUTH_DISCORD_SECRET` | （可选）Discord OAuth |
| `DATABASE_URL` | PostgreSQL 连接串，默认指向 `compose.yaml` 中的实例 |
| `NEXT_PUBLIC_API_BASE_URL` | 后端 Spring Boot 服务地址 |

## 常用脚本

| 脚本 | 作用 |
| --- | --- |
| `pnpm dev` | 启动开发服务器（Turbopack） |
| `pnpm build` / `pnpm start` | 构建并启动生产模式 |
| `pnpm typecheck` | TypeScript 严格类型检查 |
| `pnpm lint` / `pnpm lint:fix` | ESLint 检查 / 自动修复 |
| `pnpm format:write` / `format:check` | Prettier 格式化 |
| `pnpm test` | 运行 Vitest 单测（一次性） |
| `pnpm test:watch` | 监听模式 |
| `pnpm test:ui` | 启动 Vitest UI |
| `pnpm test:coverage` | 生成覆盖率报告（v8） |
| `pnpm db:generate` | 由 Drizzle schema 生成迁移 |
| `pnpm db:push` | 将 schema 直接同步到数据库（开发用） |
| `pnpm db:migrate` | 应用迁移 |
| `pnpm db:studio` | 打开 Drizzle Studio |
| `pnpm check` | `lint + typecheck` 组合 |

## 测试约定

- 测试文件位置：与被测代码同目录，命名为 `*.test.ts` / `*.test.tsx`，或集中放在 `tests/`。
- 组件测试统一使用 `@testing-library/react`；DOM 断言使用 `@testing-library/jest-dom`。
- 任何新增/修改的函数、组件、tRPC 路由都必须附带至少一个测试用例，并在 PR 中保证 `pnpm test` 通过。

## 开发约定

1. **新增功能 = 新增测试 + 更新文档**：详见 `.cursor/rules/feature-workflow.mdc`。
2. **统一通过 `~/` 别名引用 `src/`**：见 `tsconfig.json`。
3. **服务端代码只放在 `src/server/`**：使用 `import "server-only"` 防止泄漏到客户端 bundle。
4. **环境变量必须在 `src/env.js` 中声明**，不要直接读取 `process.env`。

## 参考

- [T3 Stack 文档](https://create.t3.gg/)
- [Next.js App Router](https://nextjs.org/docs/app)
- [tRPC](https://trpc.io)
- [Drizzle ORM](https://orm.drizzle.team)
- [Vitest](https://vitest.dev)
