## 系统监控模块（monitor）

系统监控模块迁移自 `thirdPart/continew-admin-ui` 的在线用户、登录日志与操作日志能力，并按本项目的 REST API、动态菜单和按钮权限模型重新落地。

---

### 1. 功能范围

| 功能 | 前端入口 | 后端接口 | 权限码 |
| --- | --- | --- | --- |
| 在线用户 | `/admin/monitor/online` | `/monitor/online` | `monitor:online:list` |
| 在线用户强退 | `/admin/monitor/online` | `DELETE /monitor/online/{token}` | `monitor:online:kickout` |
| 系统日志列表 | `/admin/monitor/log` | `/system/log` | `monitor:log:list` |
| 系统日志详情 | `/admin/monitor/log` | `GET /system/log/{id}` | `monitor:log:get` |
| 系统日志导出 | `/admin/monitor/log` | `/system/log/export/login`、`/system/log/export/operation` | `monitor:log:export` |

前端侧边栏由 `MenuSeeder` 自动初始化 “系统监控” 目录及 “在线用户 / 系统日志” 两个菜单，`RoleMenuSeeder` 会把内置菜单默认绑定给 `admin` 角色。

---

### 2. 数据模型

#### 2.1 在线会话 `idp_mon_online_session`

| 字段 | 说明 |
| --- | --- |
| `token` | JWT 原文，作为在线会话主键 |
| `jti` | JWT ID，用于和 Redis TokenStore 校验有效性 |
| `user_id` | 用户 ID |
| `username` | 用户名 |
| `nickname` | 用户昵称 |
| `ip` / `address` | 登录 IP 与展示地点；当前地址暂以 IP 回填 |
| `browser` / `os` | 根据 User-Agent 粗略识别浏览器与系统 |
| `login_time` | 登录时间 |
| `last_active_time` | 最近一次携带 token 访问接口的时间 |

#### 2.2 系统日志 `idp_mon_log`

| 字段 | 说明 |
| --- | --- |
| `trace_id` | 单次日志追踪 ID |
| `description` | 操作描述，例如 `查询 /system/user` |
| `module` | 所属模块，例如 `登录`、`用户管理` |
| `time_taken` | 请求耗时，单位毫秒 |
| `ip` / `address` | 操作 IP 与展示地点；当前地址暂以 IP 回填 |
| `browser` / `os` | 浏览器与终端系统 |
| `status` | `1=成功`，`2=失败` |
| `error_msg` | 异常信息 |
| `create_user_string` | 操作人展示名 |
| `create_time` | 日志创建时间 |
| `request_*` / `response_*` | 请求与响应的 URL、方法、头、体、状态码 |

---

### 3. 后端实现

#### 3.1 模块边界

`auth` 模块不直接调用 `monitor.internal`。登录、登出、token 活跃和操作审计都通过根包事件暴露：

- `OnlineLoginEvent`
- `OnlineTouchEvent`
- `OnlineLogoutEvent`
- `LoginAuditEvent`
- `OperationAuditEvent`

`monitor.internal.MonitorEventListener` 监听这些事件后，分别调用 `OnlineUserService` 与 `LogService` 写入在线会话和系统日志，避免 Spring Modulith 循环依赖。

#### 3.2 在线用户

- 登录成功后 `AuthServiceImpl` 发布 `OnlineLoginEvent`，写入 `idp_mon_online_session`；
- 每次 JWT 认证成功后 `JwtAuthenticationFilter` 发布 `OnlineTouchEvent`，刷新最后活跃时间；
- 登出时 `AuthController` 发布 `OnlineLogoutEvent`，移除在线会话；
- 查询在线用户时会先用 `AuthSessionService.existsJti` 清理 Redis 中已失效的会话；
- 强退时调用 `AuthSessionService.kickoutToken` 删除 TokenStore 中的 jti，再删除在线会话记录。

#### 3.3 系统日志

- 登录日志由 `AuthServiceImpl` 发布 `LoginAuditEvent` 写入，模块固定为 `登录`；
- 操作日志由 `AuditLogFilter` 捕获请求/响应并发布 `OperationAuditEvent`；
- 日志查询支持描述、模块、IP/地点、操作人、状态、时间范围过滤；
- 导出接口当前输出 CSV，前端统一通过 `http.download` 触发浏览器下载。

---

### 4. API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/monitor/online?page=1&size=10` | 在线用户分页，支持 `nickname`、`loginTime` |
| `DELETE` | `/monitor/online/{token}` | 强退在线用户 |
| `GET` | `/system/log?page=1&size=10` | 系统日志分页，支持 `description`、`module`、`ip`、`createUserString`、`status`、`createTime` |
| `GET` | `/system/log/{id}` | 系统日志详情 |
| `GET` | `/system/log/export/login` | 导出登录日志 CSV，会强制 `module=登录` |
| `GET` | `/system/log/export/operation` | 导出操作日志 CSV |

所有接口都需要 JWT，并通过 `@HasPermission` 校验对应权限码。

---

### 5. 前端实现

| 文件 | 说明 |
| --- | --- |
| `src/app/admin/monitor/online/page.tsx` | 在线用户页面，支持用户名/昵称、登录时间过滤与强退 |
| `src/app/admin/monitor/log/page.tsx` | 系统日志页面，包含登录日志 / 操作日志双 Tab |
| `src/lib/api/monitor.ts` | 监控模块 API 客户端 |
| `src/lib/api/http.ts` | 新增 `download` 方法，支持 CSV / Excel 等文件下载 |
| `src/lib/api/types.ts` | 监控模块 DTO 类型 |

页面按钮会通过 `usePermission` 控制显示：

- `monitor:online:kickout`：显示在线用户强退按钮；
- `monitor:log:get`：显示操作日志详情入口；
- `monitor:log:export`：显示日志导出按钮。

---

### 6. 测试覆盖

- 后端：
  - `OnlineUserServiceImplTest`：登录会话记录、失效会话清理、强退、活跃时间刷新；
  - `LogServiceImplTest`：登录日志记录、日志详情映射、日志不存在异常；
  - `LogControllerTest`：分页委托与登录日志导出 CSV；
  - `MonitorEventListenerTest`：auth 事件到 monitor service 的转发。
- 前端：
  - `monitor.test.ts`：在线用户、系统日志、导出 API 路径与参数；
  - `http.test.ts`：文件下载鉴权、query 序列化与临时链接清理。

