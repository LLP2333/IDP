# 权限模块（permission）

权限模块是 IDP 的 RBAC 第二层：在 “用户 — 角色” 之上额外引入 “角色 — 权限码” 关系，业务层通过 `@HasPermission` 注解 + AOP 进行细粒度授权，前端通过 `usePermission` / `PermissionGuard` 控制可见与可点。

---

## 1. 数据模型

`idp_sys_permission`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint, PK | 主键 |
| code | varchar(100), UQ | 权限码，命名规范 `资源域:对象:动作`，如 `system:user:add` |
| name | varchar(64) | 中文显示名 |
| type | int | 1=菜单，2=按钮 |
| parent_id | bigint | 父权限 id（0=顶级） |
| sort | int | 同级排序 |
| status | int | 1=启用，0=禁用 |
| is_system | boolean | 系统内置不可删 |
| description | varchar(255) | 描述 |

`idp_sys_role_permission`：联合主键 `(role_id, permission_id)`。

启动 Seeder 行为：

1. `PermissionSeeder` 灌入约 21 条系统内置权限（菜单 4 条 + 按钮 17 条），覆盖 user / role / permission / siteConfig / securityConfig / loginConfig 等模块；
2. `RolePermissionSeeder` 把所有 `isSystem=true` 的权限默认绑定到 `admin` 角色；
3. 普通业务角色不预绑定权限，由 admin 在前端 “角色管理 → 分配权限” 中按需勾选。

---

## 2. 内置权限码（示例）

| 模块 | 菜单 | 按钮 |
| --- | --- | --- |
| 用户管理 | `system:user` | `system:user:list` `system:user:add` `system:user:update` `system:user:delete` `system:user:resetPassword` `system:user:updateRole` |
| 角色管理 | `system:role` | `system:role:list` `system:role:add` `system:role:update` `system:role:delete` `system:role:assignPermission` |
| 权限管理 | `system:permission` | `system:permission:list` `system:permission:add` `system:permission:update` `system:permission:delete` |
| 系统配置 | `system:config` | `system:siteConfig:get` `system:siteConfig:update` `system:securityConfig:get` `system:securityConfig:update` `system:loginConfig:get` `system:loginConfig:update` |

---

## 3. 后端用法

### 3.1 注解

```java
@HasPermission("system:user:add")
@PostMapping
public R<Long> create(@RequestBody @Valid UserCreateReq req) { ... }
```

参数：

- `value`：String[] 权限码集合；
- `mode`：`OR`（默认，任一即可）/ `AND`（必须全部拥有）。

AOP 切面 `PermissionAspect` 从 `UserContextHolder.get()` 取 `permissionCodes` 校验，未登录抛 `BusinessException(401)`，越权抛 `BusinessException(403)`。`admin` 角色直通所有权限。

### 3.2 缓存

JWT 过滤器每次请求会按用户从 Redis 加载权限码（key=`idp:auth:perms:<userId>`，TTL 5 分钟）。角色 / 权限相关变更（`role.assignPermissions`、`permission.update/delete`、`user.update/delete/changeRole`）会调用 `AuthCacheEvictor.evictAll()` 失效全部用户缓存，保证授权变更近实时生效。

### 3.3 模块依赖

为了避免 `auth ↔ permission` 循环依赖：

- `UserContext` / `UserContextHolder` 放在 `common.security`；
- `@HasPermission` 注解所在的 `permission.annotation` 包通过 `package-info.java` 标注 `@NamedInterface("annotation")`，允许其他模块直接引用；
- `AuthCacheEvictor` 接口放在 `common.cache`，实现 `RedisAuthCacheEvictor` 落在 `auth.internal`。

---

## 4. 前端用法

### 4.1 Hook

```tsx
import { usePermission } from "~/lib/hooks/use-permission";

const { hasPermission, hasAnyPermission, hasAllPermissions, isAdmin } = usePermission();
if (!hasPermission("system:user:add")) return null;
```

### 4.2 守卫组件

```tsx
import { PermissionGuard } from "~/components/permission-guard";

<PermissionGuard codes={["system:user:add"]}>
  <Button>新增</Button>
</PermissionGuard>
```

`mode` 与后端 `@HasPermission` 对齐，默认 OR；`fallback` 可控制 “无权限” 时的占位。

### 4.3 页面

- `/admin/system/permission`：树形 CRUD，按权限码渲染按钮。
- `/admin/system/role`：角色列表中追加 “分配权限” 操作；点击弹出 `PermissionTree`，保存调用 `assignRolePermission`。
- `/admin/layout`：侧边栏导航按 `requires` 数组（任一权限即显示）过滤。

---

## 5. 注意事项

- 权限码并非自由 string：写入前端组件 / Hook 时应与后端 `PermissionSeeder` 中的常量保持一致；新增按钮务必先在 “权限管理” 创建并分配到对应角色。
- 删除内置权限会被后端拒绝（`is_system=true`）；删除非内置时需要先解绑所有角色 — 当前实现是 “直接删 + 联动清理 `role_permission`”。
- `admin` 角色与 “admin” 字符串绑定较强（写在 `UserContext#hasPermission` 与 `usePermission` 内），如需多个超级角色，请扩展为可配置集合。
