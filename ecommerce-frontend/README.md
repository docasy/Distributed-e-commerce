# 电商平台前端

基于 Vue 3 + Vite + Element Plus 的电商平台前端界面。

## 技术栈

- Vue 3.4
- Vite 5.0
- Vue Router 4.2
- Pinia 2.1
- Element Plus 2.5
- Axios 1.6

## 快速开始

### 1. 安装依赖

```bash
cd ecommerce-frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问：http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

## 功能模块

### 1. 用户认证
- 用户登录
- 用户注册
- JWT Token管理

### 2. 商品管理
- 商品列表（分页、搜索）
- 商品详情

### 3. 订单管理
- 创建订单（幂等性Token）
- 订单列表
- 订单支付
- 订单取消
- 订单详情

## 目录结构

```
src/
├── api/              # API接口
│   ├── user.js
│   ├── product.js
│   └── order.js
├── router/           # 路由配置
│   └── index.js
├── utils/            # 工具函数
│   └── request.js    # Axios封装
├── views/            # 页面组件
│   ├── Login.vue
│   ├── Layout.vue
│   ├── ProductList.vue
│   ├── ProductDetail.vue
│   └── OrderList.vue
├── App.vue
└── main.js

## 接口代理

开发环境下，所有 `/api` 请求会自动代理到后端：

```javascript
// vite.config.js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 测试账号

- 用户名：testuser
- 密码：123456

## 注意事项

1. 确保后端服务已启动（端口8080）
2. 首次登录使用测试账号
3. 创建订单需要先登录
