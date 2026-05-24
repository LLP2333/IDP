## 字典模块（dict）

字典（Dict）模块为 IDP 提供一个 “业务可配置的枚举仓库”。一处定义，前后端共用：

- 后端通过 `DictService` 暴露 CRUD + 公开接口；
- 前端通过 `useDict(code)` Hook + `DictBadge` 组件即拿即用，5 分钟 staleTime 缓存。

字典最初是为 [通知公告模块](notice.md) 服务的（分类、范围、通知方式、状态四组枚举），但它本身没有任何 notice 相关耦合，未来其它业务模块也可以直接复用。

---

### 1. 数据模型

`idp_sys_dict`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint, PK | 主键 |
| name | varchar(64) | 字典中文名 |
| code | varchar(64), 唯一 | 字典编码，前端 / 业务侧引用的稳定 key |
| description | varchar(255) | 描述 |
| is_system | boolean | 系统内置不可删；`Seeder` 预烘的字典会标 `true` |
| 审计字段 | | `created_at` / `updated_at` / `created_by` |

`idp_sys_dict_item`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint, PK | 主键 |
| dict_id | bigint | 外键，指向 `idp_sys_dict.id` |
| label | varchar(64) | 中文展示文案 |
| item_value | varchar(64) | 业务值；**注意物理列名是 `item_value`**，因为 `value` 在 H2 是保留字 |
| color | varchar(32) | 颜色 hint，由前端 `DictBadge` 映射为 Badge `tone`（primary/info/success/warning/danger） |
| sort | int | 排序，越小越靠前 |
| status | int | 1=启用、0=禁用，禁用项不会出现在前端选择器 |
| is_system | boolean | 系统内置 |
| 审计字段 | | `created_at` / `updated_at` / `created_by` |

约束：

- `(dict_id, item_value)` 唯一索引 `uk_idp_sys_dict_item_dict_value`；
- 前端选择器使用启用项 + 按 sort 升序。

---

### 2. 内置字典（DictSeeder）

| code | name | 用途 |
| --- | --- | --- |
| `notice_type` | 公告分类 | `1=公告 / 2=通知 / 3=活动 / 4=培训`（admin 可在 “字典管理” 自由扩充） |
| `notice_scope_enum` | 公告通知范围 | `1=全员 / 2=指定用户` |
| `notice_method_enum` | 公告通知方式 | `1=站内消息 / 2=弹窗` |
| `notice_status_enum` | 公告状态 | `1=草稿 / 2=待发布 / 3=已发布` |

枚举字典的存在让前端 `DictBadge` 能用同一套 “label + 颜色” 渲染任何 “整型 / 字符串状态字段”，避免 `if/else` 与硬编码颜色。

---

### 3. 接口

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/dict/list` | `system:dict:list` | 所有字典（admin 配置用） |
| GET | `/system/dict/{id}` | `system:dict:list` | 字典详情 |
| POST | `/system/dict` | `system:dict:add` | 新增字典 |
| PUT | `/system/dict/{id}` | `system:dict:update` | 修改字典（内置不可改 `code` / `is_system`） |
| DELETE | `/system/dict` | `system:dict:delete` | 批量删除（内置不可删，会级联删除明细） |
| GET | `/system/dict/{dictId}/item` | `system:dict:list` | 字典明细列表 |
| POST | `/system/dict/{dictId}/item` | `system:dict:add` | 新增明细 |
| PUT | `/system/dict/{dictId}/item/{id}` | `system:dict:update` | 修改明细 |
| DELETE | `/system/dict/{dictId}/item` | `system:dict:delete` | 批量删除明细 |
| GET | `/system/dict/{code}/item` | 已登录 | 公开接口：按 code 取启用明细，前端 `useDict` 调用 |

> 公开接口不需要 `system:dict:list` 权限，但仍需要登录态；前端只在 admin 布局加载后才会调用，登录页/校验码页不会触发。

---

### 4. 前端用法

```ts
import { useDict } from "~/lib/hooks/use-dict";
import { DictBadge } from "~/components/system/dict-badge";

const typeDict = useDict("notice_type");

return <DictBadge items={typeDict.items} value={row.type} />;
```

- `useDict(code, enabled?)` 返回 `{ items, getLabel, getColor }` 等便利方法；`enabled=false` 用于条件加载（如 Drawer 关闭时不发请求）。
- `dictQueryKey(code)` 暴露给写入字典后的页面，便于 `queryClient.invalidateQueries({ queryKey: dictQueryKey(code) })`。
- `/admin/system/dict` 页面提供字典 + 明细的可视化编辑（系统内置项禁止删除）。

---

### 5. 注意事项

- **不要直接读 `idp_sys_dict_item` 物理表**：前端 / 跨模块统一走 `useDict(code)` 与 `/system/dict/{code}/item`，可以避免 schema 变更（如 `value → item_value`）波及业务代码。
- **新增枚举字典时**：在 `DictSeeder` 追加种子，并把 `is_system=true`，避免误删；同时给每一项配 `color` 提升前端可读性。
- **颜色取值**：`primary/info/success/warning/danger` 之外的字符串会被 `DictBadge` 回落到 `default` 灰色 Badge，不会报错。
- **删除字典时的清理**：服务层在删除前会校验是否仍有明细 / 是否系统内置，避免误删；如需强制覆盖请显式在 admin 页面先删明细。
