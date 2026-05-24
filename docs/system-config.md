# 系统配置（option）

系统配置模块用来集中维护 “可在运行期由管理员热修改” 的参数。所有参数统一存放在 `idp_sys_option` 表，业务侧通过 `OptionService` 读取（带 Redis 缓存）。

模块边界由 Spring Modulith 强制：`option` 模块对外公开 `OptionService` / `PasswordPolicy` / `OptionCategory` / 实体 `SystemOption` / `model.resp.*`，其余实现细节封装在 `option.internal`。

---

## 1. 数据模型

`idp_sys_option`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint, PK | 自增主键 |
| category | enum(`SITE` / `PASSWORD` / `LOGIN`) | 配置分组 |
| name | varchar(64) | 字段名称（前端 label） |
| code | varchar(100) | 业务唯一键，命名规范 `分组前缀_业务键`（如 `SITE_TITLE`） |
| option_value | text | 当前值（允许为空；空时业务侧回落到 default_value） |
| default_value | text | 默认值 |
| description | varchar(255) | 描述（前端 help） |
| created_at / updated_at / created_by / updated_by | 标准审计字段 |

`category + code` 联合唯一。`option_value` 列名加了 `option_` 前缀是为了避开 H2 / 部分方言把 `value` 当保留字。

---

## 2. 三组配置

### 2.1 网站配置（SITE）

| code | 默认值 | 说明 |
| --- | --- | --- |
| `SITE_TITLE` | `IDP 管理系统` | 系统标题，登录页与浏览器 tab 使用 |
| `SITE_COPYRIGHT` | `Copyright © IDP` | 页脚版权 |
| `SITE_DESCRIPTION` | `通用企业级后台管理系统` | 站点简介 |
| `SITE_LOGO` | `null` | Logo 图片 Data URL |
| `SITE_FAVICON` | `null` | Favicon 图片 Data URL |

图片走 `POST /system/option/image`：

- 仅接受 `image/png` / `image/jpeg` / `image/svg+xml`；
- 单文件 ≤ 1MB；
- 入库为 base64 Data URL，前端可直接 `<img src>`。

### 2.2 安全配置（PASSWORD，即密码策略）

| code | 默认值 | 取值范围 | 含义 |
| --- | --- | --- | --- |
| `PASSWORD_ERROR_LOCK_COUNT` | `5` | 1 – 100 | 连续输错多少次后锁定 |
| `PASSWORD_ERROR_LOCK_MINUTES` | `15` | 1 – 1440 | 锁定时长（分钟） |
| `PASSWORD_EXPIRATION_DAYS` | `90` | 0 – 3650 | 密码有效期；`0` 表示永不过期 |
| `PASSWORD_EXPIRATION_WARNING_DAYS` | `7` | 0 – 365 | 到期前提醒天数；需 `≤ EXPIRATION_DAYS` |
| `PASSWORD_REPETITION_TIMES` | `3` | 0 – 24 | 新密码不能与最近 N 次旧密码相同 |
| `PASSWORD_MIN_LENGTH` | `8` | 6 – 64 | 密码最小长度 |
| `PASSWORD_REQUIRE_SYMBOLS` | `0` | `0` / `1` | 是否要求包含特殊字符 |
| `PASSWORD_ALLOW_CONTAIN_USERNAME` | `0` | `0` / `1` | 是否允许密码包含用户名 |

值校验、密码校验全部由枚举 `com.qvqw.idp.option.PasswordPolicy` 完成，前后端共用一份规则；`UserServiceImpl#create / resetPassword / changeCurrentPassword` 与 `AuthServiceImpl#login` 均会走该校验链。

### 2.3 登录配置（LOGIN）

| code | 默认值 | 说明 |
| --- | --- | --- |
| `LOGIN_CAPTCHA_ENABLED` | `0` | 是否开启图形验证码；默认关闭，管理员可手动启用 |

---

## 3. 接口

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/system/option` | 列表（按 `category` / `codes` 过滤） | `system:siteConfig:get` / `securityConfig:get` / `loginConfig:get` 任一（粗） + service 层按 category 细 |
| PUT | `/system/option` | 批量更新 | `system:siteConfig:update` / `securityConfig:update` / `loginConfig:update` 任一（粗） + service 层按 category 细 |
| PATCH | `/system/option/value` | 重置为默认值 | 同上 |
| POST | `/system/option/image` | 上传 base64 图片 | `system:siteConfig:update` |
| GET | `/system/option/site` | 公开网站配置 | 白名单 |
| GET | `/system/option/login` | 公开登录配置 | 白名单 |

公开接口的字段经过白名单过滤，绝不会泄露密码策略等敏感配置；任何写接口均会清空 Redis 缓存。

### 3.1 两层鉴权（避免跨类别越权）

Controller 上的 `@HasPermission` 是 **粗粒度 OR 门禁**：只要拥有任一 `:get` 就能进 list、任一 `:update` 就能进 update / reset。
这层只能挡掉 “完全没有任何配置权限” 的用户，**不能**阻止 “只有 LOGIN update” 的用户偷偷改 SITE / PASSWORD 参数。

因此 `OptionServiceImpl.list / update / resetValue` 内部再做一次 **fine-grained 按 category 鉴权**：

| OptionCategory | 查询权限码 | 修改权限码 |
| --- | --- | --- |
| SITE | `system:siteConfig:get` | `system:siteConfig:update` |
| PASSWORD | `system:securityConfig:get` | `system:securityConfig:update` |
| LOGIN | `system:loginConfig:get` | `system:loginConfig:update` |

规则：

- `list(?category=X)`：缺 `READ_PERM[X]` 直接 **403**；不指定 category 时返回值会按 category 自动过滤掉用户无权读的项；
- `update(reqs)`：每条参数按其 `OptionCategory` 校验对应 `WRITE_PERM`，任一项越权整批 **403** 回滚；
- `resetValue`：按 category 重置 → 直接校验对应 `WRITE_PERM`；按 codes 重置 → 先反查 codes 所属类别，逐类别校验，命中越权立即 **403**；
- `admin` 角色直通；`UserContext` 为空（内部服务调用 / 定时任务）也直通（与公开接口无关，公开接口走 `getByCategory` / `getValue` 不经此校验）。

所以单给一个角色 `system:loginConfig:update`，他**只能**改 LOGIN 类参数；想越界改 SITE / PASSWORD 会 403。

---

## 4. 缓存

- 单条：`idp:option:<code>`，TTL 30 分钟。
- 分组：`idp:option:cat:<category>`。
- 公开站点：`idp:option:public:site`、`idp:option:public:login`。
- 写操作（`update` / `resetValue`）后会按前缀 `idp:option:*` 失效；权限相关变更同步触发 `AuthCacheEvictor.evictAll()` 让 JWT 过滤器在下次请求重新加载。

---

## 5. 前端

- `src/lib/api/option.ts`：CRUD + 公开接口客户端。
- `src/components/system/site-config-form.tsx` / `security-config-form.tsx` / `login-config-form.tsx`：三个 Tab 表单。
- `src/components/ui/upload-image.tsx`：base64 上传组件，配合 `POST /system/option/image`。
- `src/app/admin/system/config/page.tsx`：聚合页（按权限渲染 Tab）。
- `src/app/login/page.tsx`：登录页首屏读取 `getSiteConfigPublic` / `getLoginConfigPublic`，决定标题 / Logo / 验证码开关。
