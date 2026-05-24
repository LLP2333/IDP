# IDP 前端

基于 Next.js 15 (App Router) 的 IDP 管理后台前端。**不依赖** NextAuth / tRPC / Drizzle —— 所有数据都通过 fetch 直连后端 Spring Boot REST API，登录态使用 JWT + Zustand 持久化到 `localStorage`。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 语言 | TypeScript 5 |
| 框架 | Next.js 15（App Router）+ React 19 |
| 样式 | Tailwind CSS v4 |
| 数据请求 | 自封装 `lib/api/http.ts` (fetch 封装) + TanStack React Query |
| 状态 | Zustand（持久化登录态） |
| 表单 | React Hook Form + Zod + `@hookform/resolvers` |
| 提示 | sonner（Toast） |
| 图标 | lucide-react |
| 测试 | Vitest + Testing Library + jsdom |
| 包管理 | pnpm 10 |

## 目录结构

```text
frontend/
├── src/
│   ├── app/
│   │   ├── login/page.tsx                # 登录页（账号密码）
│   │   ├── admin/
│   │   │   ├── layout.tsx                # 后台 Shell（侧边栏 + 顶栏 + 登出）
│   │   │   ├── page.tsx                  # 概览页
│   │   │   └── system/
│   │   │       ├── user/page.tsx         # 用户管理
│   │   │       └── role/page.tsx         # 角色管理
│   │   ├── layout.tsx                    # 根 Layout（QueryProvider + Toaster）
│   │   └── page.tsx                      # 入口：根据登录态跳 /login 或 /admin
│   ├── components/
│   │   ├── providers/query-provider.tsx
│   │   ├── ui/                           # 基础组件（Button/Input/Modal/Table/Form 等）
│   │   └── system/                       # 业务组件（user-form/role-form 等）
│   ├── lib/
│   │   ├── api/                          # http 封装 + auth/user/role API 客户端 + 类型
│   │   ├── store/auth-store.ts           # zustand 持久化登录态
│   │   └── utils.ts                      # cn / apiUrl
│   ├── styles/globals.css
│   └── env.js                            # 环境变量校验（仅 NEXT_PUBLIC_API_BASE_URL）
├── eslint.config.js
├── next.config.js
├── postcss.config.js
├── prettier.config.js
├── tsconfig.json
├── vitest.config.ts
├── vitest.setup.ts
└── package.json
```

## 环境变量

`.env`：

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

| 变量 | 说明 |
| --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | 后端 Spring Boot REST 地址，前端所有请求基地址 |

## API 客户端约定

`src/lib/api/http.ts` 提供统一的 fetch 封装：

- 自动注入 `Authorization: Bearer <token>`（来自 zustand `auth-store`）。
- 自动反序列化后端 `R<T> = {code, msg, data}` 结构，仅当 `code === 0` 时返回 `data`，否则抛出 `HttpError`。
- `401` 自动调用注册的 unauthorized 回调（默认会清空登录态并跳转 `/login`）。

API 客户端文件 → 后端接口对应：

| 文件 | 后端接口 |
| --- | --- |
| `lib/api/auth.ts` | `POST /auth/login`、`POST /auth/logout`、`GET /auth/user/info` |
| `lib/api/user.ts` | `GET/POST/PUT/DELETE /system/user` 系列 + 重置密码 / 分配角色 |
| `lib/api/role.ts` | `GET/POST/PUT/DELETE /system/role` 系列 |

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
| `pnpm check` | `lint + typecheck` 组合 |

## 测试约定

- 测试文件位置：与被测代码同目录，命名为 `*.test.ts` / `*.test.tsx`。
- 组件测试统一使用 `@testing-library/react` + `@testing-library/jest-dom`。
- 任何新增/修改的函数、组件、API 客户端都必须附带至少一个测试，并在 PR 中保证 `pnpm test` 通过。

## 开发约定

1. **新增功能 = 新增测试 + 更新文档**：详见 `.cursor/rules/feature-workflow.mdc`。
2. **统一通过 `~/` 别名引用 `src/`**：见 `tsconfig.json`。
3. **环境变量必须在 `src/env.js` 中声明**，不要直接读取 `process.env`（仅 `lib/api/http.ts` 直接读，因 server / client 同时使用）。
4. **登录态、JWT、用户信息只放在 `auth-store`**，禁止再放到其它 store 或 cookie。

## 默认登录账号

后端会自动初始化默认管理员账号 `admin / 123456`，登录后请尽快通过用户管理页重置密码。
