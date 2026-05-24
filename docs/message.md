## 站内消息模块（message）

站内消息（Message）是 IDP 提供给 “系统通知 → 用户收件箱” 的最小化能力。它故意保持极简：

- 只维护 “一条消息广播到 N 个用户” 的多对多关系；
- 用 `idp_sys_message_log` 记录每个用户的 “已读时间”，按 `read_time` 是否为空区分已读 / 未读；
- 由其他业务模块（如 [通知公告模块](notice.md)）作为 fanout 终点调用，不直接对外开放 “给某人发消息” 的 UI。

---

### 1. 数据模型

`idp_sys_message`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint, PK | 主键 |
| type | int | 消息类型，1=系统通知；预留扩展位 |
| title | varchar(150) | 标题 |
| content | text | 摘要文案 |
| path | varchar(255) | 客户端点击后跳转的相对路径（如 `/admin/system/notice/view?id=1`） |
| 审计字段 | | `created_at` / `updated_at` / `created_by` |

`idp_sys_message_log` 复合主键 `(message_id, user_id)`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| message_id | bigint | 外键 |
| user_id | bigint | 外键 |
| read_time | timestamp | 已读时间；为 `null` 时表示未读 |

---

### 2. 接口

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/message` | 已登录 | 当前用户的消息分页（可按 `isRead` 过滤） |
| GET | `/system/message/unread-count` | 已登录 | 当前用户未读条数（顶栏 bell 用） |
| POST | `/system/message/{id}/read` | 已登录 | 把某条标记为已读 |
| POST | `/system/message/read-all` | 已登录 | 一键全部标记已读 |

> 写入入口 **不暴露 HTTP 接口**，仅作为 Java API：`MessageService.publish(req, userIds)`。`userIds=null` 时取 `UserService.listEnabledUserIds()` 全员 fanout。

---

### 3. Java API

```java
@Autowired MessageService messageService;

messageService.publish(new MessageCreateReq(
        "公告通知",
        "您收到一条公告通知：" + notice.getTitle(),
        "/admin/system/notice/view?id=" + notice.getId()),
        notice.getNoticeScope() == 2 ? notice.getNoticeUsers() : null);
```

实现要点：

- 先 `save(message)` 主体；
- 再批量插入 `MessageLog`（`read_time=null`）；
- 跨模块通过 `@NamedInterface`（`message.model.req`）暴露 `MessageCreateReq`，避免破坏 Spring Modulith 边界。

---

### 4. 前端用法

- API 客户端：`frontend/src/lib/api/message.ts`；
- `/admin/message` 页面：消息列表 + 全部已读 + 点击跳转 `message.path`；
- `NotificationBell` 顶栏组件：
  - `useQuery({queryKey:["message","unread-count"]})` 拉未读数，TanStack Query 自动复用缓存；
  - 下拉中渲染最近 5 条 `useQuery({queryKey:["message",{isRead:false,page:1,size:5}]})`；
  - 点击单条消息会调 `read(id)` + 路由跳转 `message.path`。

---

### 5. 与通知公告的协作

- 通知公告发布时（立即 / 定时到点）若 `noticeMethods` 含 `SYSTEM_MESSAGE`，会调用 `MessageService.publish` 落库；
- 通知公告的 “弹窗” 路径与站内消息无关：弹窗由前端 `NoticePopup` 直接 fetch `/system/notice/popup` 取未读集合，关闭即调 `/system/notice/{id}/read` 标记为已读；
- 删除通知公告 **不会** 删除已 fanout 的站内消息（语义上 “消息已经送达，回收公告原文不影响收件箱里的快照”）。

---

### 6. 注意事项

- **fanout 量级**：当前 `publish` 一次直接落 `N` 条 `MessageLog`，单条公告全员发送 = 当前启用用户数；若用户量级到达 1w+ 级别需要拆分批写 + 异步队列。
- **已读语义**：`message_log.read_time` 单调写入，重复标记已读不会覆盖原时间。
- **`type` 字段预留**：当前仅 `1=系统通知`；如果将来要补 `2=审批通知` / `3=工单通知` 等业务消息源，可直接在 `MessageCreateReq` 上扩字段，无需新表。
