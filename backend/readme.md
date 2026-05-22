# IDP 后端

基于 Spring Boot 的 IDP 后端服务，采用 **Spring Modulith** 进行模块化拆分。

## 技术栈

| 项 | 选型 |
| --- | --- |
| 语言 | Java 21 |
| 框架 | Spring Boot 3 + Spring Modulith |
| 数据库 | PostgreSQL |
| 缓存 | Redis |
| 构建 | Maven (`./mvnw`) |
| 测试 | JUnit 5 + Spring Test |

## 目录规范

按业务边界划分包，遵循 Spring Modulith 风格：

```text
com.example.demo.order
├── OrderController.java         // 对外 HTTP 接口
├── OrderApplicationService.java // 订单应用服务
├── Order.java                   // 订单实体
├── OrderRepository.java         // 仓储/Mapper
├── OrderCreatedEvent.java       // 订单创建事件
└── internal                     // 仅模块内部可见
    ├── OrderPriceCalculator.java
    └── OrderStatusValidator.java
```

- 跨模块依赖只能通过模块根包暴露的类型。
- `internal/` 下的类不允许被其他模块引用。

## 常用命令

```bash
./mvnw spring-boot:run     # 本地启动（需先 docker compose up -d）
./mvnw test                # 运行所有测试
./mvnw verify              # 包含 Modulith 结构校验
./mvnw spring-boot:build-image  # 构建容器镜像
```

## 开发约定

1. **新增功能 = 新增测试**：所有 Service、Controller、Repository 都需附 JUnit 测试。
2. **新增功能 = 更新文档**：变更对外接口或模块边界时同步更新本 README 与 `docs/`。
3. 详见根目录 `.cursor/rules/feature-workflow.mdc`。
