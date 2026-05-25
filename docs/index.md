---
layout: home

hero:
  name: IDP 技术文档
  text: 企业后台系统设计与实现说明
  tagline: 汇总认证、权限、菜单、配置、通知、消息、监控等模块文档，便于本地静态站点阅读。
  actions:
    - theme: brand
      text: 开始阅读
      link: /auth
    - theme: alt
      text: 查看用户与角色
      link: /user-role

features:
  - title: 后端模块
    details: Spring Boot 4、Spring Modulith、PostgreSQL、Redis 与领域边界设计。
  - title: 前端入口
    details: Next.js App Router、REST API 封装、权限守卫与后台页面结构。
  - title: 运维视角
    details: 系统配置、通知公告、站内消息与监控日志能力的落地说明。
---

## 文档目录

| 模块 | 说明 |
| --- | --- |
| [认证与登录](auth.md) | JWT、Redis Token Store、验证码、登录锁定与审计流程 |
| [用户与角色管理](user-role.md) | 用户、角色、角色关联和权限聚合 |
| [菜单模块](menu.md) | 动态菜单、按钮权限、`@HasPermission` 与前端守卫 |
| [系统配置](system-config.md) | 网站配置、密码策略、登录配置和运行期缓存 |
| [字典模块](dict.md) | 可配置枚举、字典项、前后端共用字典能力 |
| [通知公告](notice.md) | 草稿、发布、定时发布、公告已读与弹窗通知 |
| [站内消息](message.md) | 消息 fanout、收件箱、未读计数和已读状态 |
| [系统监控](monitor.md) | 在线用户、强退、登录日志、操作日志和导出 |
