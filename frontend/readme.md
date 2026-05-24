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
│   │   ├── login/page.tsx                # 登录页（账号密码 + 可选验证码 + SITE 回填）
│   │   ├── admin/
│   │   │   ├── layout.tsx                # 后台 Shell（按权限过滤侧边栏 + 顶栏 + 登出）
│   │   │   ├── page.tsx                  # 概览页
│   │   │   ├── profile/
│   │   │   │   └── page.tsx              # 个人中心：基本信息 + 安全设置（含修改密码）
│   │   │   └── system/
│   │   │       ├── user/page.tsx         # 用户管理
│   │   │       ├── role/page.tsx         # 角色管理 + 分配菜单
│   │   │       ├── menu/page.tsx         # 菜单管理（树形表格 + type 联动弹窗）
│   │   │       └── config/page.tsx       # 系统配置（SITE/PASSWORD/LOGIN Tab）
│   │   ├── layout.tsx                    # 根 Layout（QueryProvider + Toaster）
│   │   └── page.tsx                      # 入口：根据登录态跳 /login 或 /admin
│   ├── components/
│   │   ├── providers/query-provider.tsx
│   │   ├── ui/                           # 基础组件（Button/Input/Modal/Tabs/Switch/UploadImage 等）
│   │   ├── system/                       # 业务表单（user/role/menu-tree/三个配置表单）
│   │   └── permission-guard.tsx          # <PermissionGuard codes=[...]>
│   ├── lib/
│   │   ├── api/                          # http 封装 + auth/user/role/option/menu API + 类型
│   │   ├── hooks/use-permission.ts       # 权限判断 Hook（admin 直通）
│   │   ├── store/auth-store.ts           # zustand 持久化登录态（含 menuTree 非持久字段）
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
| `lib/api/auth.ts` | `POST /auth/login`、`POST /auth/logout`、`GET /auth/user/info`、`GET /auth/captcha`、`POST /system/user/password`、`PUT /system/user/profile` |
| `lib/api/user.ts` | `GET/POST/PUT/DELETE /system/user` 系列 + 重置密码 / 分配角色 |
| `lib/api/role.ts` | `GET/POST/PUT/DELETE /system/role` 系列 |
| `lib/api/option.ts` | `GET/PUT/PATCH /system/option`、`POST /system/option/image`、公开 `GET /system/option/site` 与 `/system/option/login` |
| `lib/api/menu.ts` | `GET/POST/PUT/DELETE /system/menu` 系列 + `GET/PUT /system/role/{id}/menu` + `GET /auth/user/route`（前端动态侧边栏数据源） |

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
5. **所有 `export` 的函数 / Hook / 组件 / 类型必须有中文 JSDoc**；组件 Props 接口的每个字段都要 `/** ... */` 行内注释；详见 `.cursor/rules/api-doc-comments.mdc`。
6. **JSDoc 中如包含 `*/` 序列**（如 `**/model/*` 这种 glob）必须改写规避，否则 `tsc` 会报伪类型错误。

## 侧边栏：动态菜单驱动

`admin/layout.tsx` 在登录后调用 `GET /auth/user/route` 拉取用户可见菜单（{@code type=1} 目录、{@code type=2} 菜单按 sort 升序的树），按以下规则渲染：

- 顶级目录（{@code type=1}）渲染为可展开 / 折叠的分组；
- 菜单（{@code type=2}）渲染为 `Link`；当 `path` 命中当前路由时高亮；
- `isExternal=true` 渲染为 `<a target="_blank">`；
- `isHidden=true` 或 `status=0` 的节点会被过滤；
- `icon` 字段按 `ICON_MAP` 映射到 lucide-react 图标，未匹配时退化为 `Folder`。

`/admin`（概览）与 `/admin/profile`（个人中心：基本信息 + 修改密码）保持硬编码（不属于菜单管理的内容）。详细的菜单数据模型见 [`../docs/menu.md`](../docs/menu.md)。

## 个人中心 `/admin/profile`

参考 continew-admin 的个人中心页布局，由 `app/admin/profile/page.tsx` 实现：

- 左侧 “基本信息” 卡：展示 ID / 用户名 / 昵称 / 邮箱 / 手机 / 性别 / 角色，并提供 “编辑” 按钮触发 Modal 修改昵称、邮箱、手机、性别；
- 右侧 “安全设置” 卡：展示登录密码（始终已设置）、安全邮箱、安全手机三行；点击 “修改” 调起对应 Modal（密码使用专门的 “原密码 + 新密码 + 确认” 表单，邮箱 / 手机复用基本信息 Modal）；
- 修改基本信息成功后会立即 `getUserInfo` 重拉并更新 zustand store，顶栏 / 侧边栏即时反映新的昵称；
- 修改密码成功后会自动登出并跳回 `/login`，强制使用新密码重新登录。

后端配套接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `PUT` | `/system/user/profile` | 当前登录用户自助修改昵称 / 邮箱 / 手机 / 性别（任意登录用户可调用，无需 `system:user:*` 权限） |
| `POST` | `/system/user/password` | 当前登录用户自助修改密码 |
| `GET` | `/auth/user/info` | 拉取当前用户信息（含 `gender`） |

## 默认登录账号

后端会自动初始化默认管理员账号 `admin / 123456`，登录后请尽快通过用户管理页重置密码。

## 已知 breaking change（v2 菜单改造）

- 旧 `/admin/system/permission` 页面已下线，改为 `/admin/system/menu`；旧书签需要更新。
- 旧权限码 `system:permission:*` 整体废弃，统一为 `system:menu:*`；自定义角色绑定的旧权限需要在 “角色管理 → 分配菜单” 中重新分配。
- 数据库表 `idp_sys_permission` / `idp_sys_role_permission` 由 `idp_sys_menu` / `idp_sys_role_menu` 取代，本地需要 `drop database idp;` 重建。
