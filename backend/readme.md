# IDP后端


## 目录规范
遵守Spring Modulith
类似这种
```text
com.example.demo.order
├── OrderController.java        // 对外 HTTP 接口
├── OrderApplicationService.java // 订单应用服务
├── Order.java                  // 订单实体
├── OrderRepository.java        // 仓储/Mapper
├── OrderCreatedEvent.java      // 订单创建事件
└── internal
    ├── OrderPriceCalculator.java
    └── OrderStatusValidator.java
```