## 菜单模块（menu）

菜单模块在 IDP 中同时承担两类职责：

1. **前端侧边栏 / 路由元数据**：目录（{@code type=1}）+ 菜单（{@code type=2}）按 sort 升序组成树，前端动态渲染侧边栏并跳转。
2. **细粒度按钮权限**：按钮（{@code type=3}）节点的 `permission` 字段即为权限码（如 `system:user:add`），业务层通过 `@HasPermission` 注解 + AOP 完成鉴权。

`idp_sys_role_menu` 关联角色与菜单，前端 “角色管理 → 分配菜单” 可视化勾选；最终授权信息由 `RoleService#listPermissionCodesByUserId` 聚合后写入 JWT 上下文，由 `JwtAuthenticationFilter` 缓存到 Redis。

---

### 1. 数据模型

`idp_sys_menu`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint, PK | 主键 |
| title | varchar(64) | 中文标题 |
| parent_id | bigint | 父节点（0=顶级） |
| type | int | 1=目录、2=菜单、3=按钮 |
| path | varchar(255) | 路由地址；按钮类型为空，外链必须以 http(s) 开头 |
| name | varchar(64) | 组件名称（按钮类型为空） |
| component | varchar(255) | 组件路径（按钮类型为空，目录默认 `Layout`） |
| redirect | varchar(255) | 重定向地址 |
| icon | varchar(50) | 图标标识，前端按 `ICON_MAP` 映射 lucide 图标 |
| is_external | boolean | 是否外链 |
| is_cache | boolean | 是否启用 keep-alive 缓存 |
| is_hidden | boolean | 是否在侧边栏隐藏 |
| permission | varchar(100), 唯一 | 按钮权限码；其他类型为空 |
| sort | int | 同级排序，越小越靠前 |
| status | int | 1=启用，0=禁用 |
| is_system | boolean | 系统内置不可删，且 `permission/type/parentId` 不可改 |
| description | varchar(255) | 描述 |

`idp_sys_role_menu`：联合主键 `(role_id, menu_id)`，索引 `idx_idp_sys_role_menu_role` / `idx_idp_sys_role_menu_menu`。

**启动 Seeder**

1. `MenuSeeder`（{@code @Order(15)}）按 “目录 → 菜单 → 按钮” 三层灌入：1 个目录（系统管理）+ 4 个菜单（用户 / 角色 / 菜单 / 网站配置）+ 21 个按钮，共 26 条；老库里如果存在历史遗留的 “安全配置 / 登录配置” 两个 type=2 菜单，启动时会自动把它们的子按钮 reparent 到 “网站配置” 并删除多余菜单；
2. `RoleMenuSeeder`（{@code @Order(17)}）把所有 `is_system=true` 的菜单默认绑定到 `admin` 角色；
3. 普通业务角色不预绑定菜单，由 admin 在前端 “角色管理 → 分配菜单” 中按需勾选。

---

### 2. 内置权限码

| 模块 | 菜单标题（path） | 按钮权限码（`permission` 字段） |
| --- | --- | --- |
| 用户管理 | 用户管理（/admin/system/user） | `system:user:list/add/update/delete/resetPassword/updateRole` |
| 角色管理 | 角色管理（/admin/system/role） | `system:role:list/add/update/delete/assignPermission`（“分配菜单” 按钮复用了 assignPermission 这一权限码，语义保持向下兼容） |
| 菜单管理 | 菜单管理（/admin/system/menu） | `system:menu:list/add/update/delete` |
| 网站配置 | 网站配置（/admin/system/config，含站点 / 安全 / 登录三组 tab） | `system:siteConfig:get/update`、`system:securityConfig:get/update`、`system:loginConfig:get/update`（全部直接挂在 “网站配置” 菜单下） |

---

### 3. 后端用法

#### 3.1 注解

```java
@HasPermission("system:user:add")
@PostMapping
public R<Long> create(@RequestBody @Valid UserCreateReq req) { ... }
```

参数：

- `value`：String[] 权限码集合；
- `mode`：`OR`（默认，任一即可）/ `AND`（必须全部拥有）。

切面 `MenuAspect` 从 `UserContextHolder.get()` 取 `permissionCodes` 校验，未登录抛 `BusinessException(401)`，越权抛 `BusinessException(403)`。`admin` 角色直通所有权限。

#### 3.2 接口

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/menu` | `system:menu:list` | 平铺列表（按 sort 升序） |
| GET | `/system/menu/tree` | `system:menu:list` | 树形结构 |
| GET | `/system/menu/{id}` | `system:menu:list` | 详情 |
| POST | `/system/menu` | `system:menu:add` | 新增 |
| PUT | `/system/menu/{id}` | `system:menu:update` | 修改（内置菜单禁止改 type/parentId/permission） |
| DELETE | `/system/menu` | `system:menu:delete` | 批量删除（内置不可删；存在子节点不可删） |
| GET | `/system/role/{id}/menu` | `system:role:list` / `system:role:assignPermission` | 查询角色已绑定的菜单 ID |
| PUT | `/system/role/{id}/menu` | `system:role:assignPermission` | 分配角色菜单（全量覆盖，admin 角色不允许） |
| GET | `/auth/user/route` | 已登录 | 当前用户可见菜单树（type=1/2，按 sort 排序） |

#### 3.3 缓存

JWT 过滤器每次请求会按用户从 Redis 加载权限码（key=`idp:auth:perms:<userId>`，TTL 5 分钟）。任何会影响授权的变更都会调用 `AuthCacheEvictor` 让缓存失效：

- `MenuServiceImpl.create/update/delete` → `evictAll()`；
- `RoleServiceImpl.assignMenus` → `evictUsers(roleUserIds)`；
- `RoleServiceImpl.delete` → `evictAll()`；
- `UserServiceImpl.changeRoles/delete` → `evictUser(userId)`。

#### 3.4 模块依赖

为了避免循环依赖：

- `UserContext` / `UserContextHolder` 放在 `common.security`；
- `@HasPermission` 注解所在的 `menu.annotation` 包通过 `package-info.java` 标注 `@NamedInterface("annotation")`，允许其他模块直接引用；
- `MenuResp` 所在的 `menu.model.resp` 包同样标注 `@NamedInterface("model")`，允许 role / auth 模块引用；
- `AuthCacheEvictor` 接口放在 `common.cache`，实现 `RedisAuthCacheEvictor` 落在 `auth.internal`。

---

### 4. 前端用法

#### 4.1 Hook & 守卫

```tsx
import { usePermission } from "~/lib/hooks/use-permission";
import { PermissionGuard } from "~/components/permission-guard";

const { hasPermission, hasAnyPermission, hasAllPermissions, isAdmin } = usePermission();
if (!hasPermission("system:user:add")) return null;

<PermissionGuard codes={["system:user:add"]}>
  <Button>新增</Button>
</PermissionGuard>
```

#### 4.2 页面

- `/admin/system/menu`：菜单管理树形表格 + 新增 / 编辑弹窗，字段联动 `type / isExternal / path / component`；
- `/admin/system/role`：角色列表中追加 “分配菜单” 操作；点击弹出 `MenuTree`，保存调用 `assignRoleMenu`；
- `/admin/layout.tsx`：侧边栏通过 `useQuery(["auth","user-route"], getUserRoute)` 拉取动态菜单，按 `isHidden / status / sort` 过滤排序；`icon` 字符串映射到 lucide-react 图标（参考 `ICON_MAP`）。

#### 4.3 API 客户端

```ts
import {
  listMenu, getMenuTree, getMenu, createMenu, updateMenu, deleteMenu,
  getRoleMenu, assignRoleMenu, getUserRoute,
} from "~/lib/api/menu";
```

---

### 5. 注意事项

- **权限码并非自由 string**：写入前端组件 / Hook 时应与后端 `MenuSeeder` 中的常量保持一致；新增按钮务必先在 “菜单管理” 创建并分配到对应角色。
- **系统内置菜单**：被后端通过 `is_system=true` 标记，禁止删除，禁止修改 `type/parentId/permission` 三个关键字段，避免破坏鉴权语义。
- **Next.js 文件路由约束**：菜单字段保留 `path/name/component/redirect/isCache/isExternal`，但在本项目中 `component/isCache/redirect` 仅作元数据保存（不会动态注册路由），前端侧边栏只消费 `path/title/icon/isHidden/sort/type` 渲染。
- **admin 角色**：`admin` 角色与 “admin” 字符串绑定较强（写在 `UserContext#hasPermission` / `MenuAspect` / `usePermission` 内），如需多个超级角色，请扩展为可配置集合。
- **历史 `system:permission:*` 已下线**：旧权限码全部由 `system:menu:*` 取代；如果数据库里有自定义角色绑定了旧权限，需要重新分配菜单。
