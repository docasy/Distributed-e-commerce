# 快速开始指南 🚀

这是一个**5分钟快速上手**指南，帮助你快速启动项目。

## 前置条件

✅ 已安装 Docker Desktop（Windows/Mac）  
✅ 已安装 JDK 11+  
✅ 已安装 Maven 3.6+  
✅ 已安装 IDEA 或其他Java IDE

## Step 1: 启动基础服务 (2分钟)

打开PowerShell，进入项目目录：

```powershell
cd d:\Downloads\Distributed-e-commerce

# 启动所有基础服务（MySQL、Redis、RabbitMQ、Nacos等）
docker-compose up -d

# 等待30秒，让服务完全启动
Start-Sleep -Seconds 30

# 检查服务状态
docker-compose ps
```

**预期输出：**
```
NAME                     STATUS
ecommerce-mysql          Up
ecommerce-redis          Up
ecommerce-rabbitmq       Up
ecommerce-nacos          Up
ecommerce-elasticsearch  Up
```

## Step 2: 验证基础服务 (1分钟)

打开浏览器，访问以下地址：

1. **Nacos控制台**：http://localhost:8848/nacos
   - 账号：`nacos`
   - 密码：`nacos`

2. **RabbitMQ控制台**：http://localhost:15672
   - 账号：`admin`
   - 密码：`admin123`

## Step 3: 启动微服务 (2分钟)

### 方式1：使用IDEA（推荐）

1. 用IDEA打开项目文件夹
2. 等待Maven依赖下载完成
3. 依次运行以下类（点击绿色三角形）：
   - `ecommerce-user/UserApplication.java`
   - `ecommerce-product/ProductApplication.java`
   - `ecommerce-order/OrderApplication.java`
   - `ecommerce-gateway/GatewayApplication.java`

### 方式2：使用Maven命令

打开4个PowerShell窗口，分别执行：

```powershell
# 窗口1：用户服务
cd ecommerce-user
mvn spring-boot:run

# 窗口2：商品服务
cd ecommerce-product
mvn spring-boot:run

# 窗口3：订单服务
cd ecommerce-order
mvn spring-boot:run

# 窗口4：网关服务
cd ecommerce-gateway
mvn spring-boot:run
```

## Step 4: 测试接口

### 使用Postman测试

#### 1. 用户注册
```http
POST http://localhost:8080/api/user/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456",
  "phone": "13800138000",
  "nickname": "测试用户"
}
```

#### 2. 用户登录
```http
POST http://localhost:8080/api/user/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456"
}
```

**复制响应中的 `token`，后续请求需要用到**

#### 3. 查询商品列表
```http
GET http://localhost:8080/api/product/page?pageNum=1&pageSize=10
```

#### 4. 生成幂等性Token
```http
GET http://localhost:8080/api/order/idempotent-token
Authorization: <你的token>
```

#### 5. 创建订单
```http
POST http://localhost:8080/api/order/create
Authorization: <你的token>
Content-Type: application/json

{
  "productId": 1,
  "quantity": 1,
  "address": "北京市朝阳区xxx",
  "receiver": "张三",
  "receiverPhone": "13800138000",
  "idempotentToken": "<刚才获取的token>"
}
```

## 测试数据

数据库已自动初始化以下测试数据：

### 测试用户
| 用户名 | 密码 | 手机号 |
|--------|------|--------|
| testuser | 123456 | 13800138000 |
| alice | 123456 | 13900139000 |
| bob | 123456 | 13700137000 |

### 测试商品
| ID | 商品名 | 价格 | 库存 |
|----|--------|------|------|
| 1 | iPhone 15 Pro | 8999.00 | 100 |
| 2 | MacBook Pro | 14999.00 | 50 |
| 3 | AirPods Pro | 1899.00 | 200 |
| 4 | 小米14 Ultra | 6499.00 | 150 |
| 5 | 华为Mate 60 Pro | 6999.00 | 80 |

## 常见问题

### ❌ 问题1：Docker启动失败

**现象**：`docker-compose up -d` 报错

**解决**：
```powershell
# 确保Docker Desktop正在运行
# 删除旧容器重新启动
docker-compose down
docker-compose up -d
```

### ❌ 问题2：服务无法注册到Nacos

**现象**：Nacos控制台看不到服务

**解决**：
1. 检查Nacos是否启动：http://localhost:8848/nacos
2. 检查服务日志中是否有报错
3. 确认 `application.yml` 中Nacos地址正确

### ❌ 问题3：Maven依赖下载慢

**解决**：配置阿里云Maven镜像

编辑 `~/.m2/settings.xml`（如果没有则创建）：
```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### ❌ 问题4：端口冲突

**现象**：服务启动时提示端口被占用

**解决**：
```powershell
# 查看端口占用
netstat -ano | findstr "8080"

# 修改 application.yml 中的端口号
```

## Step 5: 启动前端（可选）🆕

如果你想通过可视化界面测试项目，可以启动Vue 3前端应用。

### 安装Node.js依赖

```powershell
cd ecommerce-frontend

# 首次运行需要安装依赖（约1-2分钟）
npm install
```

### 启动开发服务器

```powershell
# 启动Vite开发服务器
npm run dev
```

**预期输出：**
```
  VITE v5.0.11  ready in 500 ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
```

### 使用前端界面测试

1. **打开浏览器**：http://localhost:3000

2. **登录测试账号**（已在数据库中预置）：
   - 用户名：`testuser`
   - 密码：`123456`

3. **测试完整流程**：
   - ✅ 登录成功后，自动跳转到商品列表
   - ✅ 浏览商品，支持搜索和分页
   - ✅ 点击"立即购买"，创建订单（自动处理幂等性）
   - ✅ 查看"我的订单"，查看订单状态
   - ✅ 支付订单或取消订单
   - ✅ 点击右上角头像，退出登录

**前端特性**：
- 🔐 JWT自动管理：请求自动携带token，401自动跳转登录
- 🛡️ 幂等性保护：购买商品时自动获取幂等性令牌
- 🎨 Element Plus UI：专业的组件库，界面美观
- ⚡ Vite开发：热更新，修改代码实时生效

## 下一步

✅ 阅读 [README.md](README.md) 了解完整架构  
✅ 查看核心代码实现  
✅ 尝试修改代码并测试

## 关闭服务

```powershell
# 停止所有Docker服务
docker-compose down

# 删除所有数据（谨慎操作）
docker-compose down -v
```

---

🎉 **恭喜！你已经成功运行了分布式电商平台！**
