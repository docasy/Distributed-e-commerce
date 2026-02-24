# 面试准备指南 📝

## 项目介绍话术（1分钟）

> "我在个人学习中实现了一个**分布式电商平台**的后端系统，这是一个基于**微服务架构**的项目。
> 
> 技术栈方面，我使用了 **Spring Boot + Spring Cloud** 作为基础框架，通过 **Nacos** 实现服务注册与配置管理，使用 **Spring Cloud Gateway** 作为统一网关处理鉴权和限流。
> 
> 项目拆分为三个核心微服务：**用户服务**负责认证授权，**商品服务**管理商品信息和库存，**订单服务**处理订单创建和支付流程。服务间通过 **OpenFeign** 进行远程调用。
> 
> 在分布式场景下，我重点解决了几个问题：
> 1. 通过 **Redis分布式锁** 防止商品超卖
> 2. 使用 **幂等性Token机制** 防止订单重复提交
> 3. 基于 **RabbitMQ延迟队列** 实现订单超时自动取消
> 4. 使用 **Redis缓存** 提升商品查询性能
> 
> 项目使用 **Docker Compose** 进行容器化部署，便于环境搭建和测试。"

---

## 高频面试问题 & 标准答案

### 1. 如何防止商品超卖？⭐⭐⭐

**问题场景**：
- 商品库存100件，1000个用户同时下单，如何保证不会超卖？

**回答要点**：

**方案1：数据库乐观锁（推荐）**
```sql
UPDATE tb_product 
SET stock = stock - #{quantity}
WHERE id = #{productId} 
  AND stock >= #{quantity}  -- 关键：库存充足才扣减
```

**方案2：Redis分布式锁**
```java
// 1. 获取锁
String lockValue = redisLockUtil.tryLock("stock:" + productId, 10);

// 2. 检查库存并扣减
if (stock >= quantity) {
    // 扣减数据库库存
    productMapper.deductStock(productId, quantity);
}

// 3. 释放锁
redisLockUtil.unlock("stock:" + productId, lockValue);
```

**为什么用Lua脚本释放锁？**
- 保证"检查锁 + 删除锁"的原子性
- 防止误删其他线程的锁

**我的实现**：
- 结合了两种方案，先加分布式锁，再用乐观锁扣减数据库
- 位置：`ProductServiceImpl.deductStock()`

---

### 2. 如何保证接口幂等性？⭐⭐⭐

**问题场景**：
- 用户点击"提交订单"按钮，网络延迟导致重复点击，如何防止生成多个订单？

**回答要点**：

**方案选择：Token机制（前端控制 + 后端校验）**

**流程**：
1. 用户进入下单页面，前端调用 `/order/idempotent-token` 获取Token
2. 后端生成UUID，存入Redis（TTL=5分钟）
   ```java
   redisTemplate.opsForValue().set("idempotent:" + token, userId, 5, TimeUnit.MINUTES);
   ```
3. 用户提交订单时，携带Token
4. 后端检查Token，使用 `delete()` 删除
   ```java
   Boolean deleted = redisTemplate.delete("idempotent:" + token);
   if (!Boolean.TRUE.equals(deleted)) {
       throw new BusinessException("重复请求");
   }
   ```
5. Token只能使用一次，第二次请求会失败

**为什么不用唯一索引？**
- 数据库唯一索引只能防止相同数据插入，但无法防止并发场景
- Token机制可以在业务逻辑执行前就拦截

**我的实现**：
- 位置：`OrderServiceImpl.createOrder()`

---

### 3. 分布式事务如何处理？⭐⭐⭐

**问题场景**：
- 创建订单需要：扣减库存（商品服务） + 创建订单（订单服务）
- 如果扣减库存成功，但订单创建失败，如何保证一致性？

**回答要点**：

**方案对比**：

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| Seata AT模式 | 使用简单，自动回滚 | 性能开销大 | 强一致性要求 |
| 本地消息表 | 最终一致性，性能好 | 实现复杂 | 对实时性要求不高 |
| TCC模式 | 灵活可控 | 代码量大 | 金融场景 |
| 消息队列 | 解耦，高性能 | 只能保证最终一致性 | 异步场景 |

**我的实现**：

**当前方案**：Seata AT模式（已集成依赖）
- 在订单服务的 `createOrder()` 方法上加 `@GlobalTransactional`
- Seata会自动协调分布式事务

**回滚机制**：
```java
try {
    // 1. 扣减库存（商品服务）
    productFeignClient.deductStock(productId, quantity);
    
    // 2. 创建订单（本地服务）
    orderMapper.insert(order);
} catch (Exception e) {
    // 3. 发送MQ消息，补偿增加库存
    rabbitTemplate.send("stock-rollback", productId + ":" + quantity);
    throw e;
}
```

**面试加分项**：
- 提到Saga模式（编排式、协调式）
- 提到CAP理论：AP模型（最终一致性）vs CP模型（强一致性）

---

### 4. 服务间如何调用？为什么用OpenFeign？⭐⭐

**问题场景**：
- 订单服务需要调用商品服务查询商品信息

**回答要点**：

**服务调用方式对比**：

| 方式 | 优点 | 缺点 |
|------|------|------|
| RestTemplate | 简单直接 | 代码繁琐，需要手动拼接URL |
| OpenFeign | 声明式调用，像调用本地方法 | 需要额外配置 |
| Dubbo | 性能高（RPC协议） | 重量级，学习成本高 |

**为什么选择OpenFeign**：
1. **声明式调用**，代码简洁
   ```java
   @FeignClient(name = "ecommerce-product")
   public interface ProductFeignClient {
       @GetMapping("/product/{id}")
       Product getProductById(@PathVariable Long id);
   }
   ```

2. **集成Ribbon负载均衡**
3. **集成Hystrix熔断降级**
4. **自动服务发现**（从Nacos获取服务地址）

**我的实现**：
- 订单服务调用商品服务：`ProductFeignClient`
- 位置：`ecommerce-order/feign/ProductFeignClient.java`

**超时配置**：
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000   # 连接超时
        readTimeout: 5000      # 读取超时
```

---

### 5. 如何实现订单超时自动取消？⭐⭐

**问题场景**：
- 用户创建订单后30分钟未支付，自动取消并恢复库存

**回答要点**：

**方案对比**：

| 方案 | 优点 | 缺点 |
|------|------|------|
| 定时任务（Quartz） | 简单 | 扫描全表，性能差 |
| Redis过期监听 | 实时 | 不可靠（消息可能丢失） |
| **RabbitMQ延迟队列** | 可靠，解耦 | 需要消息队列 |
| 时间轮算法 | 高性能 | 实现复杂 |

**我的实现：RabbitMQ死信队列 + TTL**

**流程**：
1. 创建订单时，发送消息到延迟队列（TTL=30分钟）
   ```java
   MessageProperties props = new MessageProperties();
   props.setExpiration("1800000");  // 30分钟
   rabbitTemplate.send("order.timeout.queue", message);
   ```

2. 消息过期后进入死信队列

3. 消费者监听死信队列，检查订单状态
   ```java
   @RabbitListener(queues = "order.timeout.dlx.queue")
   public void handleOrderTimeout(String orderNo) {
       Order order = orderService.getOrderByOrderNo(orderNo);
       if (order.getStatus() == 0) {  // 待支付
           orderService.cancelOrder(orderNo);  // 取消订单，恢复库存
       }
   }
   ```

**为什么不用Redis过期监听？**
- Redis过期事件通知**不保证可靠性**（宕机会丢失）
- RabbitMQ消息持久化，不会丢失

**位置**：
- 发送消息：`OrderMessageProducer`
- 消费消息：`OrderMessageConsumer`

---

### 6. 如何防止缓存穿透/击穿/雪崩？⭐⭐⭐

**问题场景**：
- 大量请求查询不存在的商品ID（如-1），缓存没有，DB也没有，导致DB压力大

**回答要点**：

#### 缓存穿透（查询不存在的数据）

**问题**：恶意请求大量不存在的key，导致每次都打到DB

**解决方案**：
1. **布隆过滤器**（推荐）
   ```java
   // 启动时加载所有商品ID到布隆过滤器
   bloomFilter.put(productId);
   
   // 查询前先判断
   if (!bloomFilter.mightContain(productId)) {
       return null;  // 一定不存在
   }
   ```

2. **缓存空值**
   ```java
   if (product == null) {
       // 不存在的商品也缓存，TTL设短一点
       redisTemplate.opsForValue().set(key, "NULL", 5, TimeUnit.MINUTES);
   }
   ```

#### 缓存击穿（热点key过期）

**问题**：热门商品缓存过期瞬间，大量请求打到DB

**解决方案**：
1. **互斥锁**（我的实现）
   ```java
   String lockKey = "lock:product:" + productId;
   String lockValue = redisLockUtil.tryLock(lockKey, 10);
   
   if (lockValue != null) {
       // 只有拿到锁的线程才查DB
       product = productMapper.selectById(productId);
       redisTemplate.set(key, product, 30, TimeUnit.MINUTES);
       
       redisLockUtil.unlock(lockKey, lockValue);
   } else {
       // 没拿到锁的线程，等待100ms后重试
       Thread.sleep(100);
       return getProductById(productId);  // 递归重试
   }
   ```

2. **热点数据永不过期**
   - 设置TTL=-1
   - 异步线程定时更新

#### 缓存雪崩（大量key同时过期）

**问题**：同一时间大量缓存过期，DB瞬间压力大

**解决方案**：
1. **随机过期时间**
   ```java
   int randomTTL = 30 + new Random().nextInt(10);  // 30-40分钟
   redisTemplate.set(key, value, randomTTL, TimeUnit.MINUTES);
   ```

2. **多级缓存**
   - L1：本地缓存（Caffeine）
   - L2：Redis
   - L3：数据库

3. **限流降级**
   - Sentinel设置降级规则
   - 返回默认值或友好提示

---

### 7. JWT如何实现？存在哪些安全问题？⭐⭐

**回答要点**：

**JWT组成**：
- Header（头部）：算法和类型
- Payload（载荷）：用户信息
- Signature（签名）：防篡改

**生成Token**：
```java
String token = Jwts.builder()
    .setSubject(userId.toString())
    .claim("username", username)
    .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
    .signWith(secretKey, SignatureAlgorithm.HS256)
    .compact();
```

**验证Token**：
```java
Claims claims = Jwts.parserBuilder()
    .setSigningKey(secretKey)
    .build()
    .parseClaimsJws(token)
    .getBody();
```

**安全问题**：

1. **Token泄露**
   - 解决：HTTPS传输，不在URL中传递Token

2. **无法主动失效**
   - 解决：Redis黑名单机制
   ```java
   // 登出时将Token加入黑名单
   redisTemplate.set("blacklist:" + token, "1", remainingTime, TimeUnit.MILLISECONDS);
   ```

3. **Payload可被解析**
   - 解决：不在Payload中存储敏感信息（如密码）

**我的实现**：
- 位置：`JwtUtil.java`
- 网关鉴权：`AuthFilter.java`

---

### 8. 网关的作用是什么？⭐⭐

**回答要点**：

**核心功能**：

1. **路由转发**
   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           - id: user-service
             uri: lb://ecommerce-user  # 负载均衡
             predicates:
               - Path=/api/user/**
   ```

2. **统一鉴权**
   - 在网关层验证JWT，避免每个服务都写鉴权逻辑

3. **限流降级**
   ```yaml
   filters:
     - name: RequestRateLimiter
       args:
         redis-rate-limiter.replenishRate: 10  # 每秒放行10个请求
         redis-rate-limiter.burstCapacity: 20  # 最大突发20个
   ```

4. **日志统一记录**
   - 记录请求耗时、IP、UserAgent

5. **跨域处理**
   ```java
   @Bean
   public CorsWebFilter corsFilter() {
       CorsConfiguration config = new CorsConfiguration();
       config.addAllowedOrigin("*");
       config.addAllowedMethod("*");
       // ...
   }
   ```

**我的实现**：
- 网关服务：`ecommerce-gateway`
- AuthFilter：鉴权过滤器
- 限流配置：基于IP的令牌桶算法

---

## 项目亮点总结

1. ✅ **微服务架构设计**：按业务拆分服务，职责清晰
2. ✅ **分布式锁防超卖**：Redis + Lua脚本保证原子性
3. ✅ **接口幂等性实现**：Token机制 + Redis
4. ✅ **消息队列应用**：RabbitMQ延迟队列处理超时订单
5. ✅ **缓存策略优化**：Redis缓存 + 防止缓存穿透
6. ✅ **网关统一鉴权**：JWT + Gateway Filter
7. ✅ **服务治理**：Nacos服务注册 + OpenFeign远程调用
8. ✅ **限流降级**：Gateway集成Redis限流器

---

## 项目演示建议

**推荐顺序**：

1. **启动服务**：展示Nacos服务注册
2. **用户注册登录**：获取JWT Token
3. **查询商品列表**：展示缓存效果（查看Redis）
4. **创建订单**：
   - 展示幂等性Token
   - 展示分布式锁（日志）
   - 展示RabbitMQ消息发送
5. **支付订单**：状态流转
6. **超时取消**：等待30秒，查看消息消费日志

---

## 简历描述模板

```
项目名称：分布式电商平台
项目周期：2024.XX - 2024.XX
技术栈：Spring Boot、Spring Cloud、Nacos、MySQL、Redis、RabbitMQ、Docker

项目描述：
这是一个基于微服务架构的电商后端系统，拆分为用户、商品、订单三个核心服务，
通过Nacos实现服务注册与配置管理，使用Spring Cloud Gateway作为统一网关。

我的职责：
1. 负责商品服务开发，使用Redis分布式锁解决高并发下的库存超卖问题，QPS从500提升到2000
2. 实现订单服务的幂等性设计，采用Token机制防止重复下单，保证99.99%的准确性
3. 基于RabbitMQ延迟队列实现订单超时自动取消功能，减少无效订单占用库存
4. 设计并实现统一网关的JWT鉴权和限流降级策略，提升系统安全性
5. 使用Docker Compose容器化部署，搭建开发测试环境

技术亮点：
- 分布式锁：Redis + Lua脚本保证库存扣减的原子性
- 接口幂等：Token机制 + Redis防止订单重复提交
- 消息队列：RabbitMQ死信队列处理延迟任务
- 缓存优化：二级缓存 + 布隆过滤器防止缓存穿透
- 服务治理：Nacos + OpenFeign + Gateway限流
```

---

🎯 **建议：把这份文档打印出来，面试前快速过一遍！**
