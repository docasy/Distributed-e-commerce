# 项目跑通问题调整
## 1. 目标与结论
- 目标：按 `QUICKSTART.md` 跑通基础设施 + 4 个微服务 + 前端联调。
- 结论：已跑通。
  - 基础容器：`mysql/redis/rabbitmq/nacos/elasticsearch` 均 `Up`
  - 后端端口：`8080/8081/8082/8083` 可监听
  - 网关冒烟：`/api/user/login`、`/api/product/page`、`/api/order/idempotent-token` 均返回 `code=200`
  - 前端：Vite 已启动（因 `3000` 被占用，自动切换 `3001`）

---

## 2. 关键问题与处理记录

### 问题A：基础服务状态确认
- 现象：启动前不确定中间件是否可用。
- 定位：`docker-compose ps`。
- 结果：容器均为 `Up`；仅有 `version` 字段过时告警，不影响运行。
- 动作：保留现状，后续可清理 `docker-compose.yml` 中 `version` 字段以消除告警。

### 问题B：`ecommerce-order` Feign 无法负载均衡调用
- 现象：订单服务报 Feign 相关负载均衡错误。
- 根因：缺少 `spring-cloud-starter-loadbalancer`。
- 调整：在 `ecommerce-order/pom.xml` 增加该依赖。
- 验证：订单调用商品服务链路恢复。

### 问题C：`ecommerce-gateway` Reactive/Servlet 冲突
- 现象：网关启动报 Spring MVC 与 Gateway（Reactive）冲突。
- 根因：`ecommerce-common` 传递带入 `spring-boot-starter-web`（以及非 reactive redis）。
- 调整：在 `ecommerce-gateway/pom.xml` 引入 `ecommerce-common` 时排除：
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-redis`
  并保持网关使用 `spring-boot-starter-data-redis-reactive`。
- 验证：网关可正常启动并转发路由。

### 问题D：网关限流 `KeyResolver` Bean 冲突
- 现象：`requestRateLimiterGatewayFilterFactory` 需要单一 `KeyResolver`，但发现两个（IP、User）。
- 根因：自动注入点需要一个主 Bean。
- 调整：`ecommerce-gateway/src/main/java/com/ecommerce/gateway/config/RateLimiterConfig.java` 中将 `ipKeyResolver` 标记为 `@Primary`。
- 验证：网关限流过滤器初始化通过。

### 问题E：`user` 服务启动异常（`NoClassDefFoundError: Result`）
- 现象：`userController` 创建失败，提示找不到 `Result`。
- 根因：运行过程中曾命中旧编译产物/不一致 class 状态。
- 调整：对模块执行强制清理编译（`clean compile`）并重新启动。
- 验证：该异常消失，`user` 服务可处理请求。

### 问题F：`product/order` Swagger 启动 NPE（`documentationPluginsBootstrapper`）
- 现象：启动时出现 `PatternsRequestCondition` 相关 NPE。
- 根因：Spring Boot 2.6+ 的路径匹配策略与 Springfox 兼容问题。
- 调整：在以下文件增加
  `spring.mvc.pathmatch.matching-strategy: ant_path_matcher`
  - `ecommerce-product/src/main/resources/application.yml`
  - `ecommerce-order/src/main/resources/application.yml`
- 验证：相关 NPE 消失。

### 问题G：`order` 服务 Seata 连接失败（`127.0.0.1:8091`）
- 现象：`can not connect to services-server`。
- 根因：本地未启动 Seata Server，但订单服务引入了 Seata starter。
- 调整：
  - `ecommerce-order/src/main/resources/application.yml` 设置 `seata.enabled: false`
  - `ecommerce-order/pom.xml` 移除 `spring-cloud-starter-alibaba-seata`（本地 quickstart 场景不需要）
- 验证：订单服务可在本地场景正常运行。

### 问题H：登录接口 500（Redis 序列化 `LocalDateTime`）
- 现象：登录时写 Redis 报错：`Java 8 date/time type LocalDateTime not supported`。
- 根因：`RedisConfig` 的 `ObjectMapper` 未注册 JavaTime 模块。
- 调整：`ecommerce-common/src/main/java/com/ecommerce/common/config/RedisConfig.java`
  - `mapper.registerModule(new JavaTimeModule())`
  - `mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`
- 验证：登录接口恢复 `code=200`。

### 问题I：端口冲突/残留进程
- 现象：808x 端口偶发被占用导致服务启动失败。
- 根因：先前失败进程残留。
- 调整：按端口检查并清理占用进程，之后按模块重新启动。
- 验证：`8080/8081/8082/8083` 端口均可监听。

---

## 3. 过程中做过的主要代码调整（最终有效）
- `ecommerce-order/pom.xml`
  - 新增 `spring-cloud-starter-loadbalancer`
  - 移除 `spring-cloud-starter-alibaba-seata`
- `ecommerce-gateway/pom.xml`
  - `ecommerce-common` 依赖排除 `spring-boot-starter-web` 与 `spring-boot-starter-data-redis`
- `ecommerce-gateway/src/main/java/com/ecommerce/gateway/config/RateLimiterConfig.java`
  - `ipKeyResolver` 增加 `@Primary`
- `ecommerce-product/src/main/resources/application.yml`
  - 增加 `spring.mvc.pathmatch.matching-strategy: ant_path_matcher`
- `ecommerce-order/src/main/resources/application.yml`
  - 增加 `spring.mvc.pathmatch.matching-strategy: ant_path_matcher`
  - 增加 `seata.enabled: false`
- `ecommerce-common/src/main/java/com/ecommerce/common/config/RedisConfig.java`
  - 增加 JavaTime 序列化支持（`JavaTimeModule`）

---

## 4. 最终验证记录
- 构建：`mvn clean install -DskipTests` 成功。
- 基础容器：`docker-compose ps` 全部 `Up`。
- 后端接口冒烟：
  - `POST /api/user/login` => `200`
  - `GET /api/product/page` => `200`
  - `GET /api/order/idempotent-token` => `200`
- 前端：`npm run dev` 正常，端口自动切换到 `3001`。

---

## 5. 复用启动建议（避免再次踩坑）
1. 先起中间件：`docker-compose up -d`，确认 `docker-compose ps` 全 `Up`。
2. 优先使用模块化命令启动（避免在根 POM 误执行）：
   - `mvn -f ecommerce-user/pom.xml spring-boot:run`
   - `mvn -f ecommerce-product/pom.xml spring-boot:run`
   - `mvn -f ecommerce-order/pom.xml spring-boot:run`
   - `mvn -f ecommerce-gateway/pom.xml spring-boot:run`
3. 若端口占用，先释放 8080~8083 再重启。
4. 前端若 3000 占用，按 Vite 实际输出端口访问（如 `3001`）。
